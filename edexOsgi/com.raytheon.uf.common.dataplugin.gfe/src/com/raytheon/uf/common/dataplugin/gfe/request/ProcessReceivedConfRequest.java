/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.dataplugin.gfe.request;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Request to import the specified failed site's configuration data for service
 * backup mode.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 4, 2011            bphillip     Initial creation
 * 
 * </pre>
 * 
 * @author bphillip
 * @version 1.0
 */

@DynamicSerialize
public class ProcessReceivedConfRequest extends AbstractGfeRequest {

    @DynamicSerializeElement
    private String receivedConfFile;

    public ProcessReceivedConfRequest() {
    }

    public ProcessReceivedConfRequest(String receivedConfFile) {
        this.receivedConfFile = receivedConfFile;
    }

    /**
     * @return the receivedConfFile
     */
    public String getReceivedConfFile() {
        return receivedConfFile;
    }

    /**
     * @param receivedConfFile
     *            the receivedConfFile to set
     */
    public void setReceivedConfFile(String receivedConfFile) {
        this.receivedConfFile = receivedConfFile;
    }

}
