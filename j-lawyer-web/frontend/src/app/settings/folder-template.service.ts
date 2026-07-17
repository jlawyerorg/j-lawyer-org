import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const FOLDER_TEMPLATES_V7 = `${API_ROOT}/v7/configuration/folder-templates`;

/** A folder node within a template tree (RestfulFolderNodeV7). */
export interface FolderNode {
  id: string;
  parentId: string | null;
  name: string;
  children: FolderNode[];
}

/** A document-folder template (Aktenstruktur-Vorlage, RestfulFolderTemplateV7). */
export interface FolderTemplate {
  id: string;
  name: string;
  rootFolder: FolderNode;
}

/**
 * Document-folder templates (Aktenstruktur-Vorlagen) over the v7 configuration endpoint. Listing is
 * open to any authenticated user; all mutations require `adminRole` (enforced server-side). Business
 * errors (duplicate name, unknown folder, …) come back as HTTP 409 with a plain-text message.
 */
@Injectable({ providedIn: 'root' })
export class FolderTemplateService {
  private readonly http = inject(HttpClient);

  list(): Observable<FolderTemplate[]> {
    return this.http.get<FolderTemplate[]>(FOLDER_TEMPLATES_V7);
  }

  createTemplate(name: string): Observable<FolderTemplate> {
    return this.http.put<FolderTemplate>(FOLDER_TEMPLATES_V7, { name });
  }

  renameTemplate(id: string, name: string): Observable<FolderTemplate> {
    return this.http.post<FolderTemplate>(FOLDER_TEMPLATES_V7, { id, name });
  }

  deleteTemplate(name: string): Observable<unknown> {
    return this.http.request('delete', FOLDER_TEMPLATES_V7, { body: { name } });
  }

  cloneTemplate(sourceName: string, targetName: string): Observable<FolderTemplate> {
    return this.http.post<FolderTemplate>(`${FOLDER_TEMPLATES_V7}/clone`, { templateName: sourceName, targetName });
  }

  addFolder(templateName: string, parentId: string, name: string): Observable<FolderNode> {
    return this.http.put<FolderNode>(`${FOLDER_TEMPLATES_V7}/folders`, { templateName, parentId, name });
  }

  renameFolder(folderId: string, name: string): Observable<FolderNode> {
    return this.http.post<FolderNode>(`${FOLDER_TEMPLATES_V7}/folders`, { folderId, name });
  }

  removeFolder(folderId: string): Observable<unknown> {
    return this.http.request('delete', `${FOLDER_TEMPLATES_V7}/folders`, { body: { folderId } });
  }
}
