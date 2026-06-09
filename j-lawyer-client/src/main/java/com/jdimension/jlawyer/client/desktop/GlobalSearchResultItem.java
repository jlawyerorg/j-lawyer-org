package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.persistence.AddressBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Wrapper class for global search results of different types (cases, addresses, calendar entries).
 */
public class GlobalSearchResultItem {

    public enum ResultType {
        CASE,
        ADDRESS,
        CALENDAR
    }

    private static final ImageIcon ICON_CASE = new ImageIcon(GlobalSearchResultItem.class.getResource("/icons32/material/sharp_folder_blue_36dp.png"));
    private static final ImageIcon ICON_CASE_ARCHIVED = new ImageIcon(GlobalSearchResultItem.class.getResource("/icons32/material/folder_32dp_666666.png"));
    private static final ImageIcon ICON_ADDRESS = new ImageIcon(GlobalSearchResultItem.class.getResource("/icons32/material/baseline_perm_contact_calendar_blue_36dp.png"));
    private static final ImageIcon ICON_CALENDAR = new ImageIcon(GlobalSearchResultItem.class.getResource("/icons32/material/baseline_event_available_blue_36dp.png"));
    

    private final ResultType type;
    private final Object bean;

    public GlobalSearchResultItem(ArchiveFileBean archiveFile) {
        this.type = ResultType.CASE;
        this.bean = archiveFile;
    }

    public GlobalSearchResultItem(AddressBean address) {
        this.type = ResultType.ADDRESS;
        this.bean = address;
    }

    public GlobalSearchResultItem(ArchiveFileReviewsBean review) {
        this.type = ResultType.CALENDAR;
        this.bean = review;
    }

    public ResultType getType() {
        return type;
    }

    public Object getBean() {
        return bean;
    }

    public String getId() {
        switch (type) {
            case CASE:
                return ((ArchiveFileBean) bean).getId();
            case ADDRESS:
                return ((AddressBean) bean).getId();
            case CALENDAR:
                return ((ArchiveFileReviewsBean) bean).getArchiveFileKey().getId();
            default:
                return null;
        }
    }

    public Icon getIcon() {
        switch (type) {
            case CASE:
                if (((ArchiveFileBean) bean).isArchived()) {
                    return ICON_CASE_ARCHIVED;
                }
                return ICON_CASE;
            case ADDRESS:
                return ICON_ADDRESS;
            case CALENDAR:
                return ICON_CALENDAR;
            default:
                return null;
        }
    }

    public String getDisplayText() {
        switch (type) {
            case CASE:
                ArchiveFileBean af = (ArchiveFileBean) bean;
                return af.getFileNumber() + " " + af.getName();
            case ADDRESS:
                AddressBean ab = (AddressBean) bean;
                return ab.toDisplayName();
            case CALENDAR:
                ArchiveFileReviewsBean rev = (ArchiveFileReviewsBean) bean;
                return rev.getSummary();
            default:
                return "";
        }
    }

    public String getSecondaryText() {
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        switch (type) {
            case CASE:
                ArchiveFileBean af = (ArchiveFileBean) bean;
                return af.getReason() != null ? af.getReason() : "";
            case ADDRESS:
                AddressBean ab = (AddressBean) bean;
                StringBuilder sb = new StringBuilder();
                if (ab.getCompany() != null && !ab.getCompany().isEmpty()) {
                    sb.append(ab.getCompany());
                }
                if (ab.getCity() != null && !ab.getCity().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(ab.getCity());
                }
                return sb.toString();
            case CALENDAR:
                ArchiveFileReviewsBean rev = (ArchiveFileReviewsBean) bean;
                StringBuilder calSb = new StringBuilder();
                if (rev.getArchiveFileKey() != null) {
                    calSb.append(rev.getArchiveFileKey().getFileNumber());
                    if (rev.getArchiveFileKey().getName() != null && !rev.getArchiveFileKey().getName().isEmpty()) {
                        calSb.append(" ").append(rev.getArchiveFileKey().getName());
                    }
                }
                if (rev.getBeginDate() != null) {
                    if (calSb.length() > 0) calSb.append(", ");
                    calSb.append(df.format(rev.getBeginDate()));
                }
                return calSb.toString();
            default:
                return "";
        }
    }

    /**
     * Returns true if this is an archived case.
     */
    public boolean isArchived() {
        if (type == ResultType.CASE) {
            return ((ArchiveFileBean) bean).isArchived();
        }
        return false;
    }

    /**
     * Returns the date changed for cases, or null for other types.
     */
    public Date getDateChanged() {
        if (type == ResultType.CASE) {
            return ((ArchiveFileBean) bean).getDateChanged();
        }
        return null;
    }

    /**
     * Returns the begin date for calendar entries, or null for other types.
     */
    public Date getBeginDate() {
        if (type == ResultType.CALENDAR) {
            return ((ArchiveFileReviewsBean) bean).getBeginDate();
        }
        return null;
    }
}
