/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/gwt/shared/Attic/CmsUploadFileBean.java,v $
 * Date   : $Date: 2011/02/11 17:06:27 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.gwt.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A bean that holds the upload file infos.<p>
 * 
 * @author Ruediger Kurz
 * 
 * @version $Revision: 1.1 $
 * 
 * @since 8.0.0
 */
public class CmsUploadFileBean implements IsSerializable {

    /**
     * Stores the constants for the upload error types.<p>
     */
    public interface I_CmsUploadConstants {

        /** Key for the JSON object. */
        String KEY_BYTES_READ = "bytesRead";

        /** Key for the JSON object. */
        String KEY_CONTENT_LENGTH = "contentLength";

        /** Key for the JSON object. */
        String KEY_CURRENT_FILE = "currentFile";

        /** Key for the JSON object. */
        String KEY_SUCCESS = "success";

        /** Key for the JSON object. */
        String KEY_MESSAGE = "message";

        /** Key for the JSON object. */
        String KEY_PERCENT = "percent";

        /** Key for the JSON object. */
        String KEY_RUNNING = "running";

        /** Key for the JSON object. */
        String KEY_STACKTRACE = "stacktrace";
    }

    /** The active upload flag. */
    private boolean m_active;

    /** The list of resource names that already exist on the VFS. */
    private List<String> m_existingResourceNames;

    /** The list of filenames that are invalid. */
    private List<String> m_invalidFileNames;

    /**
     * The default constructor.<p>
     */
    public CmsUploadFileBean() {

        // noop
    }

    /**
     * The constructor with parameters.<p>
     * 
     * @param existingResourceNames list of resource names that already exist on the VFS
     * @param invalidFileNames list of filenames that are invalid
     * @param active the upload active flag
     */
    public CmsUploadFileBean(List<String> existingResourceNames, List<String> invalidFileNames, boolean active) {

        m_existingResourceNames = existingResourceNames;
        m_invalidFileNames = invalidFileNames;
        m_active = active;
    }

    /**
     * Returns the list of resource names that already exist on the VFS.<p>
     *
     * @return the list of resource names that already exist on the VFS
     */
    public List<String> getExistingResourceNames() {

        return m_existingResourceNames;
    }

    /**
     * Returns the list of filenames that are invalid.<p>
     *
     * @return the list of filenames that are invalid
     */
    public List<String> getInvalidFileNames() {

        return m_invalidFileNames;
    }

    /**
     * Returns the active.<p>
     *
     * @return the active
     */
    public boolean isActive() {

        return m_active;
    }

    /**
     * Sets the active.<p>
     *
     * @param active the active to set
     */
    public void setActive(boolean active) {

        m_active = active;
    }

    /**
     * Sets the list of resource names that already exist on the VFS.<p>
     *
     * @param existingResourceNames the list of resource names that already exist on the VFS to set
     */
    public void setExistingResourceNames(List<String> existingResourceNames) {

        m_existingResourceNames = existingResourceNames;
    }

    /**
     * Sets the list of filenames that are invalid.<p>
     *
     * @param invalidFileNames the list of filenames that are invalid to set
     */
    public void setInvalidFileNames(List<String> invalidFileNames) {

        m_invalidFileNames = invalidFileNames;
    }
}
