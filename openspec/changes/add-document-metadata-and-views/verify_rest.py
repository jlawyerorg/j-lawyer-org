#!/usr/bin/env python3
"""REST verification for add-document-metadata-and-views (tasks 11.3 server part, 11.4).

Creates its own test cases/contact/group, runs the checks and removes everything again.
"""
import base64, sys, time, uuid
import requests

B = 'http://localhost:8080/j-lawyer-io/rest'
AUTH = ('admin', 'a')
S = requests.Session(); S.auth = AUTH
TAG = 'ZZ-Verif-' + uuid.uuid4().hex[:6]
results = []
cleanup = []


def check(name, cond, detail=''):
    results.append((name, bool(cond), detail))
    print(('PASS ' if cond else 'FAIL ') + name + ('' if cond else f'   -> {detail}'))


def req(method, path, **kw):
    return S.request(method, B + path, timeout=60, **kw)


def create_case(name, group=None):
    body = {'name': name, 'reason': 'Verifikation Dokument-Metadaten'}
    if group:
        body['group'] = group
    r = req('PUT', '/v2/cases/create', json=body); r.raise_for_status()
    cid = r.json()['id']
    cleanup.append(('case', cid))
    return cid


def upload(cid, name, text):
    b64 = base64.b64encode(text.encode()).decode()
    r = req('PUT', '/v1/cases/document/create', json={'caseId': cid, 'fileName': name, 'base64content': b64})
    r.raise_for_status()
    return r.json()['id']


def v8docs(cid):
    r = req('GET', f'/v8/cases/{cid}/documents'); r.raise_for_status()
    return {d['id']: d for d in r.json()}


def doc(did):
    return req('GET', f'/v8/cases/documents/{did}').json()


def put_meta(did, **fields):
    body = {'title': None, 'keywords': [], 'receivedDate': None, 'correspondentId': None,
            'correspondentName': None, 'correspondentDirection': 0, 'parentId': None}
    body.update(fields)
    return req('PUT', f'/v8/cases/documents/{did}/metadata', json=body)


def patch(ids, **fields):
    body = {'documentIds': ids, 'changeTitle': False, 'keywordOperation': 'UNCHANGED', 'keywords': [],
            'changeReceivedDate': False, 'changeCorrespondent': False}
    body.update(fields)
    return req('PUT', '/v8/cases/documents/metadata', json=body)


def parent(did, pid):
    return req('PUT', f'/v8/cases/documents/{did}/parent', json={'parentId': pid})


def run():
    A = create_case(TAG + ' A')
    Bc = create_case(TAG + ' B')
    P = upload(A, 'mail.txt', 'Sehr geehrte Damen und Herren, anbei das Vergleichsangebot.')
    K1 = upload(A, 'anlage1.txt', 'Rechnung Nr. 1')
    K2 = upload(A, 'anlage2.txt', 'Rechnung Nr. 2')
    X = upload(A, 'sonstiges.txt', 'Notiz')
    Y = upload(A, 'kind-von-x.txt', 'Kind von X')
    OTHER = upload(Bc, 'fremd.txt', 'Dokument einer anderen Akte')

    # --- 11.4 list ---
    docs = v8docs(A)
    check('v8 list returns all documents of the case', {P, K1, K2, X, Y} <= set(docs), list(docs))
    d = docs[P]
    check('v8 list: new document has empty metadata',
          d.get('title') is None and d.get('keywords') == [] and d.get('receivedDate') is None
          and d.get('correspondentDirection') == 0 and d.get('messageCount') == 0 and d.get('parentId') is None, d)

    # --- single update + normalization ---
    rd = 1767225600000
    r = put_meta(P, title='Vergleichsangebot Gegner', keywords=[' Frist ', 'frist', 'Kostenfestsetzung'],
                 receivedDate=rd, correspondentName='RA Müller', correspondentDirection=1)
    d = doc(P)
    check('single PUT 200', r.status_code == 200, r.status_code)
    check('single PUT: title/received/correspondent stored',
          d.get('title') == 'Vergleichsangebot Gegner' and d.get('receivedDate') == rd
          and d.get('correspondentName') == 'RA Müller' and d.get('correspondentDirection') == 1, d)
    check('keywords normalized (trimmed, case-insensitive duplicates removed)',
          d.get('keywords') == ['Frist', 'Kostenfestsetzung'], d.get('keywords'))
    check('single PUT leaves content metadata unchanged', d.get('name') == 'mail.txt' and d.get('version') == 1, d)

    # --- parent / cycles ---
    check('set parent K1 -> P', parent(K1, P).status_code == 200)
    check('set parent K2 -> P', parent(K2, P).status_code == 200)
    check('cycle P -> K1 refused', parent(P, K1).status_code >= 400)
    check('self parent refused', parent(X, X).status_code >= 400)
    check('parent from another case refused', parent(K1, OTHER).status_code >= 400)
    check('v8 list shows parent relation', v8docs(A)[K1].get('parentId') == P)

    # --- bulk patch semantics ---
    r = patch([K1, K2], keywordOperation='ADD', keywords=['Beweis', 'Rechnung'])
    check('bulk ADD 200', r.status_code == 200, r.status_code)
    k1, k2 = doc(K1), doc(K2)
    check('bulk ADD adds keywords to each document', k1['keywords'] == ['Beweis', 'Rechnung'] == k2['keywords'], (k1['keywords'], k2['keywords']))
    patch([K1], keywordOperation='REMOVE', keywords=['rechnung'])
    check('bulk REMOVE (case-insensitive)', doc(K1)['keywords'] == ['Beweis'], doc(K1)['keywords'])
    patch([K2], keywordOperation='SET', keywords=['Anlage K2'])
    check('bulk SET replaces keywords', doc(K2)['keywords'] == ['Anlage K2'], doc(K2)['keywords'])
    patch([K1, K2], changeCorrespondent=True, correspondentName='Mandant Meier', correspondentDirection=1)
    k1, k2 = doc(K1), doc(K2)
    check('bulk correspondent set, other fields unchanged',
          k1['correspondentName'] == 'Mandant Meier' == k2['correspondentName']
          and k1['keywords'] == ['Beweis'] and k1.get('parentId') == P and k1.get('title') is None, k1)
    patch([K1], changeTitle=True, title='Anlage K1 – Rechnung')
    check('bulk title only when activated', doc(K1).get('title') == 'Anlage K1 – Rechnung' and doc(K2).get('title') is None)
    r = patch([K1], keywordOperation='FOO')
    check('bulk unknown keyword operation -> 400', r.status_code == 400, r.status_code)

    # --- keywords endpoint ---
    kws = req('GET', f'/v8/cases/{A}/documents/keywords').json()
    check('keywords endpoint returns keywords of the case', {'Frist', 'Kostenfestsetzung', 'Beweis', 'Anlage K2'} <= set(kws), kws)

    # --- 11.3: existing REST endpoints keep new metadata ---
    v1 = req('GET', f'/v1/cases/document/{P}').json()
    v1['name'] = 'mail-umbenannt.txt'
    r = req('PUT', '/v1/cases/document/update-metadata', json=v1)
    d = doc(P)
    check('v1 update-metadata (rename) 200', r.status_code == 200, r.status_code)
    check('v1 rename keeps title/keywords/correspondent/received',
          d['name'] == 'mail-umbenannt.txt' and d.get('title') == 'Vergleichsangebot Gegner'
          and d['keywords'] == ['Frist', 'Kostenfestsetzung'] and d.get('correspondentName') == 'RA Müller'
          and d.get('receivedDate') == rd, d)
    check('v1 rename keeps children', doc(K1).get('parentId') == P)
    r1 = req('GET', f'/v1/cases/{A}/documents'); r2 = req('GET', f'/v1/cases/{A}/documents/with-tags')
    check('v1 document lists still work', r1.status_code == 200 and r2.status_code == 200 and len(r1.json()) >= 5)

    # --- correspondent resolution ---
    email = f'{TAG.lower()}@example.org'
    # two contacts share the address; only the second one is a party of the case -> it must win
    rc = req('PUT', '/v1/contacts/create', json={'name': 'Verifikation', 'firstName': 'Andere', 'email': email})
    rc.raise_for_status(); cleanup.append(('contact', rc.json()['id']))
    rc = req('PUT', '/v1/contacts/create', json={'name': 'Verifikation', 'firstName': 'Vera', 'email': email})
    rc.raise_for_status(); contact = rc.json()['id']; cleanup.append(('contact', contact))
    rp = req('PUT', '/v1/cases/party/create', json={'caseId': A, 'addressId': contact, 'involvementType': 'Gegner'})
    check('party created for resolution test', rp.status_code == 200, rp.status_code)
    res = req('GET', f'/v8/cases/{A}/documents/correspondent',
              params={'keyType': 'email', 'key': email.upper(), 'name': 'Irgendwer', 'direction': 1}).json()
    check('correspondent resolved to case party (email case-insensitive)', res.get('correspondentId') == contact, res)
    res = req('GET', f'/v8/cases/{A}/documents/correspondent',
              params={'keyType': 'email', 'key': 'unbekannt@nowhere.invalid', 'name': 'Unbekannt GmbH', 'direction': 2}).json()
    check('unknown address -> free text, no contact', not res.get('correspondentId') and res.get('correspondentName') == 'Unbekannt GmbH'
          and res.get('correspondentDirection') == 2, res)
    r = req('GET', f'/v8/cases/{A}/documents/correspondent', params={'keyType': 'foo', 'key': 'x'})
    check('correspondent: unknown key type -> 400', r.status_code == 400, r.status_code)

    # --- messages ---
    r = req('PUT', '/v7/messages/submit', json={'sender': 'admin', 'content': 'Bitte Anlage K1 prüfen', 'caseContext': A, 'documentContext': K1})
    check('message with document context submitted', r.status_code == 200, r.status_code)
    docs = v8docs(A)
    check('messageCount per document', docs[K1]['messageCount'] == 1 and docs[K2]['messageCount'] == 0, (docs[K1]['messageCount'], docs[K2]['messageCount']))
    msgs = req('GET', f'/v8/cases/documents/{K1}/messages').json()
    check('messages of a document', len(msgs) == 1 and msgs[0].get('content') == 'Bitte Anlage K1 prüfen', msgs)

    # --- field search (index is updated asynchronously) ---
    def search(q):
        return {h['id'] for h in req('GET', '/v8/search/fulltext', params={'query': q, 'maxDocs': 50}).json()}
    kw_q = 'schlagwort:kostenfestsetzung'; t_q = 'bezeichnung:vergleichsangebot'; plain_q = 'Kostenfestsetzung'
    ok = False
    for _ in range(30):
        if P in search(kw_q) and P in search(t_q):
            ok = True; break
        time.sleep(1)
    check('field search schlagwort:/bezeichnung: finds the document', ok, (search(kw_q), search(t_q)))
    check('plain search also matches metadata', P in search(plain_q))
    check('field search does not match other documents', K2 not in search(kw_q))

    # --- delete / restore parent (children become independent) ---
    r = req('DELETE', f'/v1/cases/document/{P}/delete')
    check('delete parent (recycle bin)', r.status_code == 200, r.status_code)
    docs = v8docs(A)
    check('deleted parent not listed, children still listed', P not in docs and K1 in docs and K2 in docs)
    check('children keep parent id while parent is in the bin', docs[K1].get('parentId') == P)
    check('deleted document cannot become a parent', parent(X, P).status_code >= 400)
    r = req('PUT', f'/v7/cases/document/{P}/restore')
    docs = v8docs(A)
    check('restore parent -> relation back', r.status_code == 200 and P in docs and docs[K1].get('parentId') == P, r.status_code)

    # --- permanent delete clears parent_id (FK ON DELETE SET NULL) ---
    parent(Y, X)
    req('DELETE', f'/v1/cases/document/{X}/delete')
    r = req('DELETE', f'/v7/cases/document/{X}/permanent')
    check('permanent delete of a parent', r.status_code == 200, r.status_code)
    check('child of permanently deleted parent becomes top level', doc(Y).get('parentId') is None, doc(Y).get('parentId'))

    # --- ACL: case restricted to a group the caller is not a member of ---
    gname = TAG + '-grp'
    rg = req('PUT', '/v6/security/groups/create', json={'name': gname, 'abbreviation': TAG[-6:]})
    rg.raise_for_status(); gid = rg.json()['id']; cleanup.append(('group', gid))
    req('PUT', f'/v6/security/groups/{gid}/members/admin')
    C = create_case(TAG + ' C (ACL)', group=gname)
    CD = upload(C, 'geheim.txt', 'geheim')
    req('PUT', f'/v7/cases/{C}/groups', json=[{'id': gid, 'name': gname, 'abbreviation': TAG[-6:]}])
    check('ACL setup: admin (member) can read', req('GET', f'/v8/cases/{C}/documents').status_code == 200)
    req('DELETE', f'/v6/security/groups/{gid}/members/admin')
    acl = {
        'list': req('GET', f'/v8/cases/{C}/documents').status_code,
        'document': req('GET', f'/v8/cases/documents/{CD}').status_code,
        'keywords': req('GET', f'/v8/cases/{C}/documents/keywords').status_code,
        'messages': req('GET', f'/v8/cases/documents/{CD}/messages').status_code,
        'metadata PUT': put_meta(CD, title='hack').status_code,
        'parent PUT': parent(CD, None).status_code,
        'correspondent': req('GET', f'/v8/cases/{C}/documents/correspondent', params={'key': 'a@b.c'}).status_code,
    }
    check('ACL: non-member gets no access to v8 document endpoints', all(c in (401, 403, 404) for c in acl.values()), acl)
    rb = patch([CD], changeTitle=True, title='hack')
    check('ACL: bulk patch on foreign document refused', rb.status_code >= 400, rb.status_code)
    req('PUT', f'/v6/security/groups/{gid}/members/admin')
    check('ACL: title unchanged after refused writes', doc(CD).get('title') is None, doc(CD).get('title'))


def do_cleanup():
    # admin must be able to access the ACL case to delete it
    for kind, i in cleanup:
        if kind == 'group':
            req('PUT', f'/v6/security/groups/{i}/members/admin')
    # cases first (parties reference contacts), then contacts and groups
    order = {'case': 0, 'contact': 1, 'group': 2}
    for kind, i in sorted(reversed(cleanup), key=lambda c: order[c[0]]):
        if kind == 'case':
            r = req('DELETE', f'/v1/cases/{i}/delete')
        elif kind == 'contact':
            r = req('DELETE', f'/v2/contacts/{i}/delete')
        elif kind == 'group':
            req('DELETE', f'/v6/security/groups/{i}/members/admin')
            r = req('DELETE', f'/v6/security/groups/{i}')
        print(f'cleanup {kind} {i}: {r.status_code}')


try:
    run()
except Exception as ex:
    check('script ran without exception', False, repr(ex))
finally:
    do_cleanup()
failed = [r for r in results if not r[1]]
print(f'\n{len(results) - len(failed)}/{len(results)} checks passed')
sys.exit(1 if failed else 0)
