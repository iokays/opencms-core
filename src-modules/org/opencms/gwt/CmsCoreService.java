/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/gwt/Attic/CmsCoreService.java,v $
 * Date   : $Date: 2011/02/11 17:06:28 $
 * Version: $Revision: 1.25 $
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

package org.opencms.gwt;

import org.opencms.db.CmsResourceState;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsUser;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.flex.CmsFlexController;
import org.opencms.gwt.shared.CmsAvailabilityInfoBean;
import org.opencms.gwt.shared.CmsCategoryTreeEntry;
import org.opencms.gwt.shared.CmsContextMenuEntryBean;
import org.opencms.gwt.shared.CmsCoreData;
import org.opencms.gwt.shared.CmsCoreData.AdeContext;
import org.opencms.gwt.shared.CmsListInfoBean;
import org.opencms.gwt.shared.CmsPrincipalBean;
import org.opencms.gwt.shared.CmsUploadFileBean;
import org.opencms.gwt.shared.CmsUploadProgessInfo;
import org.opencms.gwt.shared.CmsValidationQuery;
import org.opencms.gwt.shared.CmsValidationResult;
import org.opencms.gwt.shared.rpc.I_CmsCoreService;
import org.opencms.gwt.upload.CmsUploadBean;
import org.opencms.gwt.upload.CmsUploadException;
import org.opencms.gwt.upload.CmsUploadListener;
import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsMessages;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsContextInfo;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsCategory;
import org.opencms.relations.CmsCategoryService;
import org.opencms.scheduler.CmsScheduledJobInfo;
import org.opencms.scheduler.jobs.CmsPublishScheduledJob;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsRole;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.CmsWorkplaceAction;
import org.opencms.workplace.explorer.CmsExplorerContextMenu;
import org.opencms.workplace.explorer.CmsExplorerContextMenuItem;
import org.opencms.workplace.explorer.CmsExplorerTypeSettings;
import org.opencms.workplace.explorer.CmsResourceUtil;
import org.opencms.workplace.explorer.menu.CmsMenuItemVisibilityMode;
import org.opencms.workplace.explorer.menu.CmsMenuRule;
import org.opencms.workplace.explorer.menu.I_CmsMenuItemRule;
import org.opencms.xml.sitemap.CmsSitemapManager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.InvalidFileNameException;
import org.apache.commons.fileupload.util.Streams;

/**
 * Provides general core services.<p>
 * 
 * @author Michael Moossen
 * @author Ruediger Kurz
 * 
 * @version $Revision: 1.25 $ 
 * 
 * @since 8.0.0
 * 
 * @see org.opencms.gwt.CmsCoreService
 * @see org.opencms.gwt.shared.rpc.I_CmsCoreService
 * @see org.opencms.gwt.shared.rpc.I_CmsCoreServiceAsync
 */
public class CmsCoreService extends CmsGwtService implements I_CmsCoreService {

    /** Serialization uid. */
    private static final long serialVersionUID = 5915848952948986278L;

    /**
     * Internal helper method for getting a validation service.<p>
     * 
     * @param name the class name of the validation service
     *  
     * @return the validation service 
     * 
     * @throws CmsException if something goes wrong 
     */
    public static I_CmsValidationService getValidationService(String name) throws CmsException {

        try {
            Class<?> cls = Class.forName(name, false, I_CmsValidationService.class.getClassLoader());
            if (!I_CmsValidationService.class.isAssignableFrom(cls)) {
                throw new CmsIllegalArgumentException(Messages.get().container(
                    Messages.ERR_VALIDATOR_INCORRECT_TYPE_1,
                    name));
            }
            return (I_CmsValidationService)cls.newInstance();
        } catch (ClassNotFoundException e) {
            throw new CmsException(Messages.get().container(Messages.ERR_VALIDATOR_INSTANTIATION_FAILED_1, name), e);
        } catch (InstantiationException e) {
            throw new CmsException(Messages.get().container(Messages.ERR_VALIDATOR_INSTANTIATION_FAILED_1, name), e);
        } catch (IllegalAccessException e) {
            throw new CmsException(Messages.get().container(Messages.ERR_VALIDATOR_INSTANTIATION_FAILED_1, name), e);
        }
    }

    /**
     * Instantiates a class given its name using its default constructor.<p>
     * 
     * Also checks whether the class with the given name is the subclass of another class/interface.<p>
     * 
     * 
     * @param <T> the type of the interface/class passed as a parameter
     *  
     * @param anInterface the interface or class against which the class should be checked 
     * @param className the name of the class 
     * @return a new instance of the class
     * 
     * @throws CmsException if the instantiation fails
     */
    public static <T> T instantiate(Class<T> anInterface, String className) throws CmsException {

        try {
            Class<?> cls = Class.forName(className, false, anInterface.getClassLoader());
            if (!anInterface.isAssignableFrom(cls)) {
                // class was found, but does not implement the interface 
                throw new CmsIllegalArgumentException(Messages.get().container(
                    Messages.ERR_INSTANTIATION_INCORRECT_TYPE_2,
                    className,
                    anInterface.getName()));
            }

            // we use another variable so we don't have to put the @SuppressWarnings on the method itself 
            @SuppressWarnings("unchecked")
            Class<T> typedClass = (Class<T>)cls;
            return typedClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new CmsException(Messages.get().container(Messages.ERR_INSTANTIATION_FAILED_1, className), e);
        } catch (InstantiationException e) {
            throw new CmsException(Messages.get().container(Messages.ERR_INSTANTIATION_FAILED_1, className), e);
        } catch (IllegalAccessException e) {
            throw new CmsException(Messages.get().container(Messages.ERR_INSTANTIATION_FAILED_1, className), e);
        }
    }

    /**
     * Returns a new configured service instance.<p>
     * 
     * @param request the current request
     * 
     * @return a new service instance
     */
    public static CmsCoreService newInstance(HttpServletRequest request) {

        CmsCoreService srv = new CmsCoreService();
        srv.setCms(CmsFlexController.getCmsObject(request));
        srv.setRequest(request);
        return srv;
    }

    /**
     * Cancels the upload.<p>
     */
    public void cancelUpload() {

        if (getRequest().getSession().getAttribute(CmsUploadBean.SESSION_ATTRIBUTE_LISTENER_ID) != null) {
            CmsUUID listenerId = (CmsUUID)getRequest().getSession().getAttribute(
                CmsUploadBean.SESSION_ATTRIBUTE_LISTENER_ID);
            CmsUploadListener listener = CmsUploadBean.getCurrentListener(listenerId);
            if ((listener != null) && !listener.isCanceled()) {
                listener.setException(new CmsUploadException("Upload canceled by the user."));
            }
        }
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#checkUploadFiles(java.util.List, java.lang.String)
     */
    public CmsUploadFileBean checkUploadFiles(List<String> fileNames, String targetFolder) {

        List<String> existingResourceNames = new ArrayList<String>();
        List<String> invalidFileNames = new ArrayList<String>();
        boolean isActive = false;

        // check if there is an active upload
        if (getRequest().getSession().getAttribute(CmsUploadBean.SESSION_ATTRIBUTE_LISTENER_ID) == null) {

            // check for existing files
            for (String fileName : fileNames) {

                try {
                    Streams.checkFileName(fileName);
                    String newResName = CmsResource.getName(fileName.replace('\\', '/'));
                    String newResPath = getNewResourceName(newResName, targetFolder);
                    if (getCmsObject().existsResource(newResPath, CmsResourceFilter.IGNORE_EXPIRATION)) {
                        existingResourceNames.add(fileName);
                    }
                } catch (InvalidFileNameException e) {
                    invalidFileNames.add(fileName);
                }
            }
        } else {
            isActive = true;
        }
        return new CmsUploadFileBean(existingResourceNames, invalidFileNames, isActive);
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#createUUID()
     */
    public CmsUUID createUUID() {

        return new CmsUUID();
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#getAvailabilityInfo(org.opencms.util.CmsUUID)
     */
    public CmsAvailabilityInfoBean getAvailabilityInfo(CmsUUID structureId) throws CmsRpcException {

        try {
            CmsResource res = getCmsObject().readResource(structureId, CmsResourceFilter.IGNORE_EXPIRATION);
            return getAvailabilityInfo(res);
        } catch (CmsException e) {
            error(e);
            return null; // will never be reached 
        }
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#getAvailabilityInfo(java.lang.String)
     */
    public CmsAvailabilityInfoBean getAvailabilityInfo(String vfsPath) throws CmsRpcException {

        try {
            CmsResource res = getCmsObject().readResource(vfsPath, CmsResourceFilter.IGNORE_EXPIRATION);
            return getAvailabilityInfo(res);
        } catch (CmsException e) {
            error(e);
            return null; // will never be reached 
        }
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#getCategories(java.lang.String, boolean, java.util.List)
     */
    public CmsCategoryTreeEntry getCategories(String fromPath, boolean includeSubCats, List<String> refPaths)
    throws CmsRpcException {

        CmsObject cms = getCmsObject();
        CmsCategoryService catService = CmsCategoryService.getInstance();

        List<String> repositories = new ArrayList<String>();
        if ((refPaths != null) && !refPaths.isEmpty()) {
            for (String refPath : refPaths) {
                repositories.addAll(catService.getCategoryRepositories(getCmsObject(), refPath));
            }
        } else {
            repositories.add(CmsCategoryService.CENTRALIZED_REPOSITORY);
        }

        CmsCategoryTreeEntry result = null;
        try {
            result = new CmsCategoryTreeEntry(fromPath);
            // get the categories
            List<CmsCategory> categories = catService.readCategoriesForRepositories(
                cms,
                fromPath,
                includeSubCats,
                repositories);
            // convert them to a tree structure
            CmsCategoryTreeEntry parent = result;
            for (CmsCategory category : categories) {
                CmsCategoryTreeEntry current = new CmsCategoryTreeEntry(category);
                String parentPath = CmsResource.getParentFolder(current.getPath());
                if (!parentPath.equals(parent.getPath())) {
                    parent = findCategory(result, parentPath);
                }
                parent.addChild(current);
            }
        } catch (Throwable e) {
            error(e);
        }
        return result;
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#getContextMenuEntries(java.lang.String, org.opencms.gwt.shared.CmsCoreData.AdeContext)
     */
    public List<CmsContextMenuEntryBean> getContextMenuEntries(String uri, AdeContext context) throws CmsRpcException {

        List<CmsContextMenuEntryBean> result = null;
        CmsObject cms = getCmsObject();
        switch (context) {
            case containerpage:
                cms.getRequestContext().setAttribute(
                    I_CmsMenuItemRule.ATTR_CONTEXT_INFO,
                    I_CmsCoreService.CONTEXT_CONTAINERPAGE);
                break;
            case sitemap:
                cms.getRequestContext().setAttribute(
                    I_CmsMenuItemRule.ATTR_CONTEXT_INFO,
                    I_CmsCoreService.CONTEXT_SITEMAP);
                break;
            default:
                // nothing to do here
        }

        try {
            CmsResourceUtil[] resUtil = new CmsResourceUtil[1];
            resUtil[0] = new CmsResourceUtil(cms, cms.readResource(uri));

            // the explorer type settings
            CmsExplorerTypeSettings settings = null;

            // get the context menu configuration for the given selection mode
            CmsExplorerContextMenu contextMenu;

            // get the explorer type setting for the first resource
            try {
                settings = OpenCms.getWorkplaceManager().getExplorerTypeSetting(resUtil[0].getResourceTypeName());
            } catch (Throwable e) {
                error(e);
            }
            if ((settings == null) || !settings.isEditable(cms, resUtil[0].getResource())) {
                // the user has no access to this resource type
                // could be configured in the opencms-vfs.xml or in the opencms-modules.xml
                return Collections.<CmsContextMenuEntryBean> emptyList();
            }
            // get the context menu
            contextMenu = settings.getContextMenu();

            // transform the context menu into beans
            List<CmsContextMenuEntryBean> allEntries = transformToMenuEntries(contextMenu.getAllEntries(), resUtil);

            // filter the result
            result = filterEntries(allEntries);

        } catch (Throwable e) {
            error(e);
        }
        return result;
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#getPageInfo(org.opencms.util.CmsUUID)
     */
    public CmsListInfoBean getPageInfo(CmsUUID structureId) throws CmsRpcException {

        try {
            CmsResource res = getCmsObject().readResource(structureId, CmsResourceFilter.IGNORE_EXPIRATION);
            return getPageInfo(res);
        } catch (CmsException e) {
            error(e);
            return null; // will never be reached 
        }
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#getPageInfo(java.lang.String)
     */
    public CmsListInfoBean getPageInfo(String vfsPath) throws CmsRpcException {

        try {
            CmsResource res = getCmsObject().readResource(vfsPath, CmsResourceFilter.IGNORE_EXPIRATION);
            return getPageInfo(res);
        } catch (CmsException e) {
            error(e);
            return null; // will never be reached 
        }
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#getResourceState(java.lang.String)
     */
    public CmsResourceState getResourceState(String path) throws CmsRpcException {

        CmsObject cms = getCmsObject();
        try {
            CmsResourceState result;
            try {
                CmsResource res = cms.readResource(path);
                result = res.getState();
            } catch (CmsVfsResourceNotFoundException e) {
                result = CmsResourceState.STATE_DELETED;
            }
            return result;
        } catch (CmsException e) {
            error(e);
            return null; // will never be reached 
        }
    }

    /**
     * Returns the upload progress information.<p>
     * 
     * @return the upload progress information
     */
    public CmsUploadProgessInfo getUploadProgressInfo() {

        CmsUploadProgessInfo info = new CmsUploadProgessInfo(0, 0, false, 0, 0);
        if (getRequest().getSession().getAttribute(CmsUploadBean.SESSION_ATTRIBUTE_LISTENER_ID) != null) {
            CmsUUID listenerId = (CmsUUID)getRequest().getSession().getAttribute(
                CmsUploadBean.SESSION_ATTRIBUTE_LISTENER_ID);
            CmsUploadListener listener = CmsUploadBean.getCurrentListener(listenerId);
            if (listener != null) {
                info = listener.getInfo();
            }
        }
        return info;
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#lock(java.lang.String)
     */
    public String lock(String uri) throws CmsRpcException {

        CmsObject cms = getCmsObject();
        try {
            cms.lockResource(uri);
        } catch (CmsException e) {
            return e.getLocalizedMessage(OpenCms.getWorkplaceManager().getWorkplaceLocale(cms));
        } catch (Throwable e) {
            error(e);
        }
        return null;
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#lockTemp(java.lang.String)
     */
    public String lockTemp(String uri) throws CmsRpcException {

        CmsObject cms = getCmsObject();
        try {
            cms.lockResourceTemporary(uri);
        } catch (CmsException e) {
            return e.getLocalizedMessage(OpenCms.getWorkplaceManager().getWorkplaceLocale(cms));
        } catch (Throwable e) {
            error(e);
        }
        return null;
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#lockTempAndCheckModification(java.lang.String, long)
     */
    public String lockTempAndCheckModification(String uri, long modification) throws CmsRpcException {

        CmsObject cms = getCmsObject();
        try {
            // check time stamp
            if (cms.readResource(uri).getDateLastModified() != modification) {
                return Messages.get().container(Messages.ERR_RESOURCE_MODIFIED_AFTER_OPEN_1, uri).key();
            }
        } catch (Throwable e) {
            error(e);
        }
        // lock
        return lockTemp(uri);
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#prefetch()
     */
    public CmsCoreData prefetch() {

        CmsObject cms = getCmsObject();
        String navigationUri = CmsSitemapManager.getNavigationUri(cms, getRequest());
        String uploadUri = OpenCms.getLinkManager().substituteLinkForUnknownTarget(cms, CmsUploadBean.UPLOAD_JSP_URI);
        CmsCoreData data = new CmsCoreData(
            OpenCms.getSystemInfo().getOpenCmsContext(),
            cms.getRequestContext().getSiteRoot(),
            cms.getRequestContext().getLocale().toString(),
            OpenCms.getWorkplaceManager().getWorkplaceLocale(cms).toString(),
            cms.getRequestContext().getUri(),
            navigationUri,
            uploadUri,
            System.currentTimeMillis());
        return data;
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#setAvailabilityInfo(org.opencms.util.CmsUUID, org.opencms.gwt.shared.CmsAvailabilityInfoBean)
     */
    public void setAvailabilityInfo(CmsUUID structureId, CmsAvailabilityInfoBean bean) throws CmsRpcException {

        try {
            CmsResource res = getCmsObject().readResource(structureId, CmsResourceFilter.IGNORE_EXPIRATION);
            setAvailabilityInfo(res.getRootPath(), bean);
        } catch (CmsException e) {
            error(e);
        }
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#setAvailabilityInfo(java.lang.String, org.opencms.gwt.shared.CmsAvailabilityInfoBean)
     */
    public void setAvailabilityInfo(String uri, CmsAvailabilityInfoBean bean) throws CmsRpcException {

        // get the cms object
        CmsObject cms = getCmsObject();

        try {
            String resourceSitePath = cms.getRequestContext().removeSiteRoot(uri);
            modifyPublishScheduled(resourceSitePath, bean.getDatePubScheduled());
            modifyAvailability(resourceSitePath, bean.getDateReleased(), bean.getDateExpired());
            modifyNotification(
                resourceSitePath,
                bean.getNotificationInterval(),
                bean.isNotificationEnabled(),
                bean.isModifySiblings());
        } catch (CmsException e) {
            error(e);
        }
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#translateUrlName(java.lang.String)
     */
    public String translateUrlName(String urlName) {

        String result = getCmsObject().getRequestContext().getFileTranslator().translateResource(urlName);
        result = result.replace('/', '_');
        return result;
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#unlock(java.lang.String)
     */
    public String unlock(String uri) throws CmsRpcException {

        CmsObject cms = getCmsObject();
        try {
            CmsResource resource = cms.readResource(uri);
            CmsLock lock = cms.getLock(resource);
            if (lock.isUnlocked()) {
                return null;
            }
            cms.unlockResource(uri);
        } catch (CmsException e) {
            return e.getLocalizedMessage(OpenCms.getWorkplaceManager().getWorkplaceLocale(cms));
        } catch (Throwable e) {
            error(e);
        }
        return null;
    }

    /**
     * Performs a batch of validations and returns the results.<p>
     * 
     * @param validationQueries a map from field names to validation queries
     * 
     * @return a map from field names to validation results
     *  
     * @throws CmsRpcException if something goes wrong 
     */
    public Map<String, CmsValidationResult> validate(Map<String, CmsValidationQuery> validationQueries)
    throws CmsRpcException {

        try {
            Map<String, CmsValidationResult> result = new HashMap<String, CmsValidationResult>();
            for (Map.Entry<String, CmsValidationQuery> queryEntry : validationQueries.entrySet()) {
                String fieldName = queryEntry.getKey();
                CmsValidationQuery query = queryEntry.getValue();
                result.put(fieldName, validate(query.getValidatorId(), query.getValue(), query.getConfig()));
            }
            return result;
        } catch (Throwable e) {
            error(e);
        }
        return null;
    }

    /**
     * @see org.opencms.gwt.shared.rpc.I_CmsCoreService#validate(java.lang.String, java.util.Map, java.util.Map, java.lang.String)
     */
    public Map<String, CmsValidationResult> validate(
        String formValidatorClass,
        Map<String, CmsValidationQuery> validationQueries,
        Map<String, String> values,
        String config) throws CmsRpcException {

        try {
            I_CmsFormValidator formValidator = instantiate(I_CmsFormValidator.class, formValidatorClass);
            return formValidator.validate(getCmsObject(), validationQueries, values, config);
        } catch (Throwable e) {
            error(e);
        }
        return null;
    }

    /**
     * Filters the collection of menu entry beans.<p>
     * 
     * <ul>
     * <li>removes unnecessary separators</li>
     * <li>filters sub menus also</li>
     * <li>adds visible entries to the result</li>
     * </ul>
     * 
     * @see org.opencms.gwt.shared.CmsContextMenuEntryBean
     * 
     * @param allEntries the entries to filter
     * 
     * @return the filtered list of menu entries
     */
    private List<CmsContextMenuEntryBean> filterEntries(List<CmsContextMenuEntryBean> allEntries) {

        // the resulting list
        List<CmsContextMenuEntryBean> result = new ArrayList<CmsContextMenuEntryBean>();
        CmsContextMenuEntryBean lastBean = null;

        // iterate over the list of collected menu entries to do the filtering
        for (CmsContextMenuEntryBean entry : allEntries) {
            if (entry.isVisible()) {
                // only if the entry is enabled
                if (entry.isSeparator()) {
                    if (!result.isEmpty()) {
                        // the entry is a separator and it isn't the first entry in the menu
                        if ((lastBean != null) && !lastBean.isSeparator()) {
                            // and there are no two separators behind each other
                            // add the separator
                            result.add(entry);
                        }
                    }
                } else if ((entry.getSubMenu() != null) && !entry.getSubMenu().isEmpty()) {
                    // the entry has a sub menu, so filter the entries of the sub menu
                    entry.setSubMenu(filterEntries(entry.getSubMenu()));
                    // add the entry with sub menu
                    result.add(entry);
                } else {
                    // it's a common entry, so add it
                    result.add(entry);
                }
                // store the last entry to check the separator
                lastBean = entry;
            }
        }
        // after the filtering is finished, remove the last separator if it is existent
        if (result.size() > 1) {
            if (result.get(result.size() - 1).isSeparator()) {
                result.remove(result.size() - 1);
            }
        }
        return result;
    }

    /**
     * FInds a category in the given tree.<p>
     * 
     * @param tree the the tree to search in
     * @param path the path to search for
     * 
     * @return the category with the given path or <code>null</code> if not found
     */
    private CmsCategoryTreeEntry findCategory(CmsCategoryTreeEntry tree, String path) {

        // we assume that the category to find is descendant of tree
        CmsCategoryTreeEntry parent = tree;
        if (path.equals(parent.getPath())) {
            return parent;
        }
        boolean found = true;
        while (found) {
            List<CmsCategoryTreeEntry> children = parent.getChildren();
            if (children == null) {
                return null;
            }
            // since the categories are sorted it is faster to go backwards
            found = false;
            for (int i = children.size() - 1; i >= 0; i--) {
                CmsCategoryTreeEntry child = children.get(i);
                if (path.equals(child.getPath())) {
                    return child;
                }
                if (path.startsWith(child.getPath())) {
                    parent = child;
                    found = true;
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Returns a bean that contains the infos for the {@link org.opencms.gwt.client.ui.CmsAvailabilityDialog}.<p>
     * 
     * @param res the resource to get the availability infos for
     * 
     * @return a bean for the {@link org.opencms.gwt.client.ui.CmsAvailabilityDialog}
     * 
     * @throws CmsRpcException if something goes wrong
     */
    private CmsAvailabilityInfoBean getAvailabilityInfo(CmsResource res) throws CmsRpcException {

        CmsObject cms = getCmsObject();
        try {
            CmsAvailabilityInfoBean result = new CmsAvailabilityInfoBean();

            result.setPageInfo(getPageInfo(res));

            String resourceSitePath = cms.getRequestContext().removeSiteRoot(res.getRootPath());
            result.setVfsPath(resourceSitePath);

            I_CmsResourceType type = OpenCms.getResourceManager().getResourceType(res.getTypeId());
            result.setResType(type.getTypeName());

            result.setDateReleased(res.getDateReleased());
            result.setDateExpired(res.getDateExpired());

            String notificationInterval = cms.readPropertyObject(
                res,
                CmsPropertyDefinition.PROPERTY_NOTIFICATION_INTERVAL,
                false).getValue();
            if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(notificationInterval)) {
                result.setNotificationInterval(Integer.valueOf(notificationInterval).intValue());
            }

            String notificationEnabled = cms.readPropertyObject(
                res,
                CmsPropertyDefinition.PROPERTY_ENABLE_NOTIFICATION,
                false).getValue();
            if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(notificationEnabled)) {
                result.setNotificationEnabled(Boolean.valueOf(notificationEnabled).booleanValue());
            }

            result.setHasSiblings(cms.readSiblings(resourceSitePath, CmsResourceFilter.ALL).size() > 1);

            result.setResponsibles(getResponsibles(res.getRootPath()));

            return result;
        } catch (CmsException e) {
            error(e);
            return null; // will never be reached 
        }
    }

    /**
     * Locks the given resource and returns the lock.<p>
     * 
     * @param resource the resource to lock
     * 
     * @return the lock
     * 
     * @throws CmsException if something goes wrong
     */
    private CmsLock getLockIfPossible(String resource) throws CmsException {

        // lock the resource in the current project
        CmsLock lock = getCmsObject().getLock(resource);
        // prove is current lock from current but not in current project
        if ((lock != null)
            && lock.isOwnedBy(getCmsObject().getRequestContext().currentUser())
            && !lock.isOwnedInProjectBy(
                getCmsObject().getRequestContext().currentUser(),
                getCmsObject().getRequestContext().currentProject())) {
            // file is locked by current user but not in current project
            // change the lock from this file
            getCmsObject().changeLock(resource);
        }
        // lock resource from current user in current project
        getCmsObject().lockResource(resource);
        // get current lock
        lock = getCmsObject().getLock(resource);
        return lock;
    }

    /**
     * Returns the VFS path for the given filename and folder.<p>
     * 
     * @param fileName the filename to combine with the folder
     * @param folder the folder to combine with the filename
     * 
     * @return the VFS path for the given filename and folder
     */
    private String getNewResourceName(String fileName, String folder) {

        return folder + getCmsObject().getRequestContext().getFileTranslator().translateResource(fileName);
    }

    /**
     * Returns a bean to display the {@link org.opencms.gwt.client.ui.CmsListItemWidget}.<p>
     * 
     * @param res the resource to get the page info for
     * 
     * @return a bean to display the {@link org.opencms.gwt.client.ui.CmsListItemWidget}.<p>
     * 
     * @throws CmsRpcException if something goes wrong
     */
    private CmsListInfoBean getPageInfo(CmsResource res) throws CmsRpcException {

        CmsObject cms = getCmsObject();
        try {
            CmsListInfoBean result = new CmsListInfoBean();

            result.setResourceState(res.getState());

            String resourceSitePath = cms.getRequestContext().removeSiteRoot(res.getRootPath());

            String title = cms.readPropertyObject(res, CmsPropertyDefinition.PROPERTY_TITLE, false).getValue();
            if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(title)) {
                result.setTitle(title);
            } else {
                result.setTitle("No title attribute set for this resource");
            }
            result.setSubTitle(resourceSitePath);

            String export = cms.readPropertyObject(res, CmsPropertyDefinition.PROPERTY_EXPORT, true).getValue();
            if ("true".equals(export)) {
                result.setPageIcon(CmsListInfoBean.PageIcon.export);
            }
            String secure = cms.readPropertyObject(res, CmsPropertyDefinition.PROPERTY_SECURE, true).getValue();
            if ("true".equals(secure)) {
                result.setPageIcon(CmsListInfoBean.PageIcon.secure);
            }
            Map<String, String> additionalInfo = new LinkedHashMap<String, String>();
            String resTypeName = OpenCms.getResourceManager().getResourceType(res.getTypeId()).getTypeName();
            String key = OpenCms.getWorkplaceManager().getExplorerTypeSetting(resTypeName).getKey();
            Locale currentLocale = getCmsObject().getRequestContext().getLocale();
            String resTypeNiceName = OpenCms.getWorkplaceManager().getMessages(currentLocale).key(key);
            additionalInfo.put("Type", resTypeNiceName);
            result.setAdditionalInfo(additionalInfo);

            return result;
        } catch (CmsException e) {
            error(e);
            return null; // will never be reached 
        }
    }

    /**
     * Returns a map of principals of responsible users together with the resource path where the
     * responsibility was found.<p> 
     * 
     * @param vfsPath the path pointing on the resource to get the responsible users for
     * 
     * @return a map of principal beans
     * 
     * @throws CmsRpcException if something goes wrong
     */
    private Map<CmsPrincipalBean, String> getResponsibles(String vfsPath) throws CmsRpcException {

        Map<CmsPrincipalBean, String> result = new HashMap<CmsPrincipalBean, String>();
        List<CmsResource> parentResources = new ArrayList<CmsResource>();

        CmsObject cms = getCmsObject();
        String resourceSitePath = cms.getRequestContext().removeSiteRoot(vfsPath);
        try {
            // get all parent folders of the current file
            parentResources = cms.readPath(resourceSitePath, CmsResourceFilter.IGNORE_EXPIRATION);
        } catch (CmsException e) {
            error(e);
        }

        for (CmsResource resource : parentResources) {
            String storedSiteRoot = cms.getRequestContext().getSiteRoot();
            String sitePath = cms.getRequestContext().removeSiteRoot(resource.getRootPath());
            try {

                cms.getRequestContext().setSiteRoot("/");
                List<CmsAccessControlEntry> entries = cms.getAccessControlEntries(resource.getRootPath(), false);
                for (CmsAccessControlEntry ace : entries) {
                    if (ace.isResponsible()) {
                        I_CmsPrincipal principal = cms.lookupPrincipal(ace.getPrincipal());
                        if (principal != null) {
                            CmsPrincipalBean prinBean = new CmsPrincipalBean(
                                principal.getName(),
                                principal.getDescription(),
                                principal.isGroup());
                            if (!resource.getRootPath().equals(vfsPath)) {
                                if (resource.getRootPath().startsWith(storedSiteRoot)) {
                                    result.put(prinBean, sitePath);
                                } else {
                                    result.put(prinBean, resource.getRootPath());
                                }
                            } else {
                                result.put(prinBean, null);
                            }
                        }
                    }
                }
            } catch (CmsException e) {
                error(e);
            } finally {
                cms.getRequestContext().setSiteRoot(storedSiteRoot);
            }
        }
        return result;
    }

    /**
     * Collects the matching rules of all sub items of a parent context menu entry.<p>
     * 
     * @param item the context menu item to check the sub items for
     * @param itemRules the collected rules for the sub items
     * @param resourceUtil the resources to be checked against the rules
     */
    private void getSubItemRules(
        CmsExplorerContextMenuItem item,
        List<I_CmsMenuItemRule> itemRules,
        CmsResourceUtil[] resourceUtil) {

        for (CmsExplorerContextMenuItem subItem : item.getSubItems()) {

            if (subItem.isParentItem()) {
                // this is a parent item, recurse into sub items
                getSubItemRules(subItem, itemRules, resourceUtil);
            } else if (CmsExplorerContextMenuItem.TYPE_ENTRY.equals(subItem.getType())) {
                // this is a standard entry, get the matching rule to add to the list
                String subItemRuleName = subItem.getRule();
                CmsMenuRule subItemRule = OpenCms.getWorkplaceManager().getMenuRule(subItemRuleName);
                I_CmsMenuItemRule rule = subItemRule.getMatchingRule(getCmsObject(), resourceUtil);
                if (rule != null) {
                    itemRules.add(rule);
                }
            }
        }
    }

    /**
     * Modifies the availability of the given resource.<p>
     * 
     * @param vfsPath the resource to change
     * @param dateReleased the date released
     * @param dateExpired the date expired
     * 
     * @throws CmsException if something goes wrong
     */
    private void modifyAvailability(String vfsPath, long dateReleased, long dateExpired) throws CmsException {

        getLockIfPossible(vfsPath);
        // modify release and expire date of the resource if needed
        getCmsObject().setDateReleased(vfsPath, dateReleased, false);
        getCmsObject().setDateExpired(vfsPath, dateExpired, false);
    }

    /**
     * Modifies the notification properties of the given resource.<p>
     * 
     * @param resourceSitePath the site path of the resource to modify
     * @param notificationInterval the modification interval
     * @param notificationEnabled signals whether the notification is enabled or disabled
     * @param modifySiblings signals whether siblings should be also modified
     * 
     * @throws CmsException if something goes wrong
     */
    private void modifyNotification(
        String resourceSitePath,
        int notificationInterval,
        boolean notificationEnabled,
        boolean modifySiblings) throws CmsException {

        List<CmsResource> resources = new ArrayList<CmsResource>();
        if (modifySiblings) {
            // modify all siblings of a resource
            resources = getCmsObject().readSiblings(resourceSitePath, CmsResourceFilter.IGNORE_EXPIRATION);
        } else {
            // modify only resource without siblings
            resources.add(getCmsObject().readResource(resourceSitePath, CmsResourceFilter.IGNORE_EXPIRATION));
        }
        for (CmsResource curResource : resources) {
            String resourcePath = getCmsObject().getRequestContext().removeSiteRoot(curResource.getRootPath());
            // lock resource if auto lock is enabled
            getLockIfPossible(resourcePath);
            // write notification settings
            writeProperty(
                resourcePath,
                CmsPropertyDefinition.PROPERTY_NOTIFICATION_INTERVAL,
                String.valueOf(notificationInterval));
            writeProperty(
                resourcePath,
                CmsPropertyDefinition.PROPERTY_ENABLE_NOTIFICATION,
                String.valueOf(notificationEnabled));
        }

    }

    /**
     * Modifies the publish scheduled.<p>
     * 
     * Creates a temporary project and adds the given resource to it. Afterwards a scheduled job is created
     * and the project is assigned to it. Then the publish job is enqueued.<p>
     * 
     * @param resourceSitePath the site path of the resource to modify 
     * @param pubDate the date when the resource should be published
     * 
     * @throws CmsException if something goes wrong
     */
    private void modifyPublishScheduled(String resourceSitePath, long pubDate) throws CmsException {

        if (pubDate != CmsAvailabilityInfoBean.DATE_PUBLISH_SCHEDULED_DEFAULT) {

            CmsObject cms = getCmsObject();

            String resource = resourceSitePath;
            CmsUser user = getCmsObject().getRequestContext().currentUser();
            Locale locale = getCmsObject().getRequestContext().getLocale();
            Date date = new Date(pubDate);

            // make copies from the admin cmsobject and the user cmsobject
            // get the admin cms object
            CmsWorkplaceAction action = CmsWorkplaceAction.getInstance();
            CmsObject cmsAdmin = action.getCmsAdminObject();
            // get the user cms object

            // set the current user site to the admin cms object
            cmsAdmin.getRequestContext().setSiteRoot(cms.getRequestContext().getSiteRoot());

            // create the temporary project, which is deleted after publishing
            // the publish scheduled date in project name
            String dateTime = CmsDateUtil.getDateTime(date, DateFormat.SHORT, locale);
            // the resource name to publish scheduled
            String resName = CmsResource.getName(resource);
            CmsMessages messages = OpenCms.getWorkplaceManager().getMessages(locale);
            String projectName = messages.key(
                org.opencms.workplace.commons.Messages.GUI_PUBLISH_SCHEDULED_PROJECT_NAME_2,
                new Object[] {resName, dateTime});

            // the HTML encoding for slashes is necessary because of the slashes in english date time format
            // in project names slahes are not allowed, because these are separators for organizaional units
            projectName = projectName.replace("/", "&#47;");
            // create the project
            CmsProject tmpProject = cmsAdmin.createProject(
                projectName,
                "",
                CmsRole.WORKPLACE_USER.getGroupName(),
                CmsRole.PROJECT_MANAGER.getGroupName(),
                CmsProject.PROJECT_TYPE_TEMPORARY);
            // make the project invisible for all users
            tmpProject.setHidden(true);
            // write the project to the database
            cmsAdmin.writeProject(tmpProject);
            // set project as current project
            cmsAdmin.getRequestContext().setCurrentProject(tmpProject);
            cms.getRequestContext().setCurrentProject(tmpProject);

            // copy the resource to the project
            cmsAdmin.copyResourceToProject(resource);

            // get the lock if possible
            getLockIfPossible(resource);

            // create a new scheduled job
            CmsScheduledJobInfo job = new CmsScheduledJobInfo();
            // the job name
            String jobName = projectName;
            // set the job parameters
            job.setJobName(jobName);
            job.setClassName("org.opencms.scheduler.jobs.CmsPublishScheduledJob");
            // create the cron expression
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            String cronExpr = ""
                + calendar.get(Calendar.SECOND)
                + " "
                + calendar.get(Calendar.MINUTE)
                + " "
                + calendar.get(Calendar.HOUR_OF_DAY)
                + " "
                + calendar.get(Calendar.DAY_OF_MONTH)
                + " "
                + (calendar.get(Calendar.MONTH) + 1)
                + " "
                + "?"
                + " "
                + calendar.get(Calendar.YEAR);
            // set the cron expression
            job.setCronExpression(cronExpr);
            // set the job active
            job.setActive(true);
            // create the context info
            CmsContextInfo contextInfo = new CmsContextInfo();
            contextInfo.setProjectName(projectName);
            contextInfo.setUserName(cmsAdmin.getRequestContext().currentUser().getName());
            // create the job schedule parameter
            SortedMap<String, String> params = new TreeMap<String, String>();
            // the user to send mail to
            params.put(CmsPublishScheduledJob.PARAM_USER, user.getName());
            // the job name
            params.put(CmsPublishScheduledJob.PARAM_JOBNAME, jobName);
            // the link check
            params.put(CmsPublishScheduledJob.PARAM_LINKCHECK, "true");
            // add the job schedule parameter
            job.setParameters(params);
            // add the context info to the scheduled job
            job.setContextInfo(contextInfo);
            // add the job to the scheduled job list
            OpenCms.getScheduleManager().scheduleJob(cmsAdmin, job);
        }
    }

    /**
     * Returns a list of menu entry beans.<p>
     * 
     * Takes the given List of explorer context menu items and converts them to context menu entry beans.<p>
     * 
     * @see org.opencms.gwt.shared.CmsContextMenuEntryBean
     * @see org.opencms.workplace.explorer.CmsExplorerContextMenuItem
     * 
     * @param items the menu items 
     * @param resUtil a resource utility array
     * 
     * @return a list of menu entries
     */
    private List<CmsContextMenuEntryBean> transformToMenuEntries(
        List<CmsExplorerContextMenuItem> items,
        CmsResourceUtil[] resUtil) {

        // the resulting list
        List<CmsContextMenuEntryBean> result = new ArrayList<CmsContextMenuEntryBean>();

        // get the workplace message bundle
        CmsMessages messages = OpenCms.getWorkplaceManager().getMessages(getCmsObject().getRequestContext().getLocale());

        for (CmsExplorerContextMenuItem item : items) {

            CmsContextMenuEntryBean bean = new CmsContextMenuEntryBean();

            if (!CmsExplorerContextMenuItem.TYPE_SEPARATOR.equals(item.getType())) {
                // this item is no separator (common entry or sub menu entry)

                // set the label to the bean
                if (item.getKey() != null) {
                    bean.setLabel(messages.key(item.getKey()));
                }

                // get the mode and set the bean
                CmsMenuItemVisibilityMode mode = CmsMenuItemVisibilityMode.VISIBILITY_INVISIBLE;
                String itemRuleName = item.getRule();
                if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(itemRuleName)) {
                    CmsMenuRule rule = OpenCms.getWorkplaceManager().getMenuRule(itemRuleName);
                    if (rule != null) {
                        // get the first matching rule to apply for visibility
                        I_CmsMenuItemRule itemRule = rule.getMatchingRule(getCmsObject(), resUtil);
                        if (itemRule != null) {
                            if (item.isParentItem()) {
                                // get the rules for the sub items
                                List<I_CmsMenuItemRule> itemRules = new ArrayList<I_CmsMenuItemRule>(
                                    item.getSubItems().size());
                                getSubItemRules(item, itemRules, resUtil);
                                I_CmsMenuItemRule[] itemRulesArray = new I_CmsMenuItemRule[itemRules.size()];
                                // determine the visibility for the parent item
                                mode = itemRule.getVisibility(
                                    getCmsObject(),
                                    resUtil,
                                    itemRules.toArray(itemRulesArray));
                            } else {
                                mode = itemRule.getVisibility(getCmsObject(), resUtil);
                            }
                        }
                    }
                }

                // set the visibility to the bean
                bean.setVisible(!mode.isInVisible());

                // set the activate info to the bean
                if (item.isParentItem()) {
                    // parent entries that have visible sub entries are always active
                    bean.setActive(true);
                } else {
                    // common entries can be activated or de-activated
                    bean.setActive(mode.isActive());
                    if (CmsStringUtil.isNotEmpty(mode.getMessageKey())) {
                        bean.setReason(messages.key(CmsEncoder.escapeXml(mode.getMessageKey())));
                    }
                }

                // get the JSP-URI and set it to the bean
                String jspPath = item.getUri();
                if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(jspPath)) {
                    if (item.getUri().startsWith("/")) {
                        jspPath = OpenCms.getLinkManager().substituteLink(getCmsObject(), item.getUri());
                    } else {
                        jspPath = OpenCms.getLinkManager().substituteLink(
                            getCmsObject(),
                            CmsWorkplace.PATH_WORKPLACE + item.getUri());
                    }
                }
                bean.setJspPath(jspPath);

                // get the name of the item and set it to the bean
                bean.setName(item.getName());

                // get the image-URI and set it to the bean
                String imagePath = item.getIcon();
                if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(imagePath)) {
                    if (item.getIcon().startsWith("/")) {
                        imagePath = OpenCms.getLinkManager().substituteLink(getCmsObject(), item.getIcon());
                    } else {
                        imagePath = OpenCms.getLinkManager().substituteLink(
                            getCmsObject(),
                            CmsWorkplace.PATH_WORKPLACE + item.getIcon());
                    }
                }
                bean.setImagePath(imagePath);
            }

            if (item.isParentItem()) {
                // this item has a sub menu
                bean.setSubMenu(transformToMenuEntries(item.getSubItems(), resUtil));
            }

            if (CmsExplorerContextMenuItem.TYPE_SEPARATOR.equals(item.getType())) {
                // this item is a separator
                bean.setVisible(true);
                bean.setSeparator(true);
            }

            result.add(bean);
        }
        return result;
    }

    /**
     * Internal helper method for validating a single value.<p>
     * 
     * @param validator the class name of the validation service 
     * @param value the value to validate 
     * @param config the configuration for the validation service
     *  
     * @return the result of the validation 
     * 
     * @throws Exception if something goes wrong 
     */
    private CmsValidationResult validate(String validator, String value, String config) throws Exception {

        I_CmsValidationService validationService = getValidationService(validator);
        return validationService.validate(getCmsObject(), value, config);
    }

    /**
     * Writes a property value for a resource.<p>
     * 
     * @param resourcePath the path of the resource
     * @param propertyName the name of the property
     * @param propertyValue the new value of the property
     * 
     * @throws CmsException if something goes wrong
     */
    private void writeProperty(String resourcePath, String propertyName, String propertyValue) throws CmsException {

        if (CmsStringUtil.isEmpty(propertyValue)) {
            propertyValue = CmsProperty.DELETE_VALUE;
        }

        CmsProperty newProp = new CmsProperty();
        newProp.setName(propertyName);
        CmsProperty oldProp = getCmsObject().readPropertyObject(resourcePath, propertyName, false);
        if (oldProp.isNullProperty()) {
            // property value was not already set
            if (OpenCms.getWorkplaceManager().isDefaultPropertiesOnStructure()) {
                newProp.setStructureValue(propertyValue);
            } else {
                newProp.setResourceValue(propertyValue);
            }
        } else {
            if (oldProp.getStructureValue() != null) {
                newProp.setStructureValue(propertyValue);
                newProp.setResourceValue(oldProp.getResourceValue());
            } else {
                newProp.setResourceValue(propertyValue);
            }
        }

        newProp.setAutoCreatePropertyDefinition(true);

        String oldStructureValue = oldProp.getStructureValue();
        String newStructureValue = newProp.getStructureValue();
        if (CmsStringUtil.isEmpty(oldStructureValue)) {
            oldStructureValue = CmsProperty.DELETE_VALUE;
        }
        if (CmsStringUtil.isEmpty(newStructureValue)) {
            newStructureValue = CmsProperty.DELETE_VALUE;
        }

        String oldResourceValue = oldProp.getResourceValue();
        String newResourceValue = newProp.getResourceValue();
        if (CmsStringUtil.isEmpty(oldResourceValue)) {
            oldResourceValue = CmsProperty.DELETE_VALUE;
        }
        if (CmsStringUtil.isEmpty(newResourceValue)) {
            newResourceValue = CmsProperty.DELETE_VALUE;
        }

        // change property only if it has been changed            
        if (!oldResourceValue.equals(newResourceValue) || !oldStructureValue.equals(newStructureValue)) {
            getCmsObject().writePropertyObject(resourcePath, newProp);
        }
    }
}
