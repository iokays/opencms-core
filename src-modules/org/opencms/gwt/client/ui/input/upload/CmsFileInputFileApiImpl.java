/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/gwt/client/ui/input/upload/Attic/CmsFileInputFileApiImpl.java,v $
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

package org.opencms.gwt.client.ui.input.upload;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.InputElement;

/**
 * The HTML5 file input implementation.<p>
 * 
 * @author Ruediger Kurz
 * 
 * @version $Revision: 1.1 $
 * 
 * @since 8.0.0
 */
public class CmsFileInputFileApiImpl implements I_CmsFileInputService {

    /**
     * @see org.opencms.gwt.client.ui.input.upload.I_CmsFileInputService#getFiles(com.google.gwt.dom.client.InputElement)
     */
    @Override
    public native JsArray<CmsFileInfo> getFiles(InputElement inputElement) /*-{
        return inputElement.files;
    }-*/;

    /**
     * @see org.opencms.gwt.client.ui.input.upload.I_CmsFileInputService#isAllowMultipleFiles(com.google.gwt.dom.client.InputElement)
     */
    @Override
    public boolean isAllowMultipleFiles(InputElement inputElement) {

        return inputElement.hasAttribute("multiple");
    }

    /**
     * @see org.opencms.gwt.client.ui.input.upload.I_CmsFileInputService#setAllowMultipleFiles(com.google.gwt.dom.client.InputElement, boolean)
     */
    @Override
    public void setAllowMultipleFiles(InputElement inputElement, boolean allow) {

        if (allow) {
            inputElement.setAttribute("multiple", "");
        } else {
            inputElement.removeAttribute("multiple");
        }
    }

    /**
     * @see org.opencms.gwt.client.ui.input.upload.I_CmsFileInputService#supportsFileAPI()
     */
    @Override
    public boolean supportsFileAPI() {

        return true;
    }
}
