/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Michaël Vachette
 */

package org.nuxeo.presales.disney;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;


@Scope(ScopeType.CONVERSATION)
@Name("xmlImportActions")
@Install(precedence = Install.FRAMEWORK)
public class XMLImportActions implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(XMLImportActions.class);
    
    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    protected File xmlFile;

    protected String xmlFileName;

    public void uploadListener(FileUploadEvent event) throws Exception {
        UploadedFile item = event.getUploadedFile();
        // FIXME: check if this needs to be tracked for deletion
        xmlFile = File.createTempFile("FileManageActionsFile", null);
        InputStream in = event.getUploadedFile().getInputStream();
        org.nuxeo.common.utils.FileUtils.copyToFile(in, xmlFile);
        xmlFileName = FilenameUtils.getName(item.getName());
    }

    public void importXMLFile() {
        if (xmlFile != null) {
            XMLImporterService importer = Framework.getLocalService(XMLImporterService.class);
            try {
                DocumentModel dm = navigationContext.getCurrentDocument();
                importer.importDocuments(dm,xmlFile);
                refreshUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getImportingXMLFilename() {
        return xmlFileName;
    }
    
    
    public void refreshUI() {
        try {
            navigationContext.invalidateCurrentDocument();
            DocumentModel dm = navigationContext.getCurrentDocument();
            if (dm != null) {
                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED, dm);
                Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, dm);
            }
        } catch (ClientException e) {
            log.error("Error invalidating current document " + "on navigation context", e);
        }
    }
    
    
    @Observer(EventNames.NAVIGATE_TO_DOCUMENT)
    public void resetState() {
        xmlFile = null;
        xmlFileName = null;
    }
}
