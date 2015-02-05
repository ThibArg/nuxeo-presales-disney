/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */

package org.nuxeo.presales.disney.operations;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.convert.api.ConversionService;

/**
 * 
 */
@Operation(id = AddVideoCommentOp.ID, category = Constants.CAT_DOCUMENT, label = "Dysney: Add Video Comment", description = "WARNING: Assumes current document is a video, there is a video_comments schema, etc.")
public class AddVideoCommentOp {

    public static final String ID = "Dysney.AddVideoComment";

    private static final Log log = LogFactory.getLog(AddVideoCommentOp.class);

    public static final String CONVERTER = "ffmpeg-extract-one-frame"; //"ffmpegExtractOneFrame";

    public static final String XPATH_VIDEO_COMMENTS = "vico:comments";

    @Context
    protected CoreSession session;

    @Context
    protected ConversionService conversionService;

    @Param(name = "time", required = true)
    protected String time;

    @Param(name = "comment", required = true)
    protected String comment;

    @Param(name = "save", required = false)
    protected boolean save;

    @OperationMethod
    public DocumentModel run(DocumentModel inDoc) {

        if (!inDoc.hasSchema("file")) {
            throw new ClientException("Document should have the 'file' schema");
        }
        if (!inDoc.hasSchema("video_comments")) {
            throw new ClientException(
                    "Document should have the 'video_comments' (Disney specific) schema");
        }

        Blob theVideo = (Blob) inDoc.getPropertyValue("file:content");
        if (theVideo == null) {
            return inDoc; // no error here
        }

        // Check if we already have a comment at this time
        // We get an array of Complex values, which means an array of
        // Map<String, Serializable> where the String is the name of the
        // field
        Map<String, Serializable>  theEntry = new HashMap<String, Serializable>();
        int idx = -1;
        boolean wasThere = false;
        ArrayList<Map<String, Serializable>> values = (ArrayList<Map<String, Serializable>>) inDoc.getPropertyValue(XPATH_VIDEO_COMMENTS);
        if(values != null) {
            for(Map<String, Serializable> oneEntry : values) {
                idx += 1;
                String t = (String) oneEntry.get("time");
                if(t.equals(time)) {
                    theEntry = oneEntry;
                    wasThere = true;
                    break;
                }
            }
        }
        
        theEntry.put("time",  time);
        theEntry.put("comment", comment);
        
        // Use Blob.RunConverter to extract the picture
        Blob frame = extractFrame(theVideo, time);
        theEntry.put("frame", (Serializable)frame);
            
        Property complexMeta = inDoc.getProperty(XPATH_VIDEO_COMMENTS);
        if(wasThere) {
            complexMeta.setValue(idx, theEntry);
        } else {
            complexMeta.addValue(theEntry);
        }
        
        if(save) {
            inDoc = session.saveDocument(inDoc);
        }

        return inDoc;
    }
    
    protected Blob extractFrame(Blob inVideo, String inTime) {
                
        Map<String, Serializable> params = new HashMap<>();
        params.put("position", inTime);
        params.put("targetFileName", inVideo.getFilename() + "-" + inTime + ".png");
        
        BlobHolder holder = conversionService.convert(CONVERTER, new SimpleBlobHolder(inVideo), params);
                
        return holder.getBlob();
    }

}
