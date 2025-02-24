/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.connector.operations;

import com.azure.core.http.rest.Response;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.connection.AzureStorageConnectionHandler;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.exceptions.InvalidConfigurationException;
import org.wso2.carbon.connector.util.*;
import org.wso2.carbon.connector.util.Error;

import javax.xml.stream.XMLStreamException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the update meta data operation.
 */
public class UpdateMetaData extends AbstractAzureMediator {

    @Override
    public void execute(MessageContext messageContext, String responseVariable, Boolean overwriteBody) {
        String connectionName = getProperty(messageContext, AzureConstants.CONNECTION_NAME, String.class, false);
        String fileSystemName = getMediatorParameter(messageContext, AzureConstants.FILE_SYSTEM_NAME, String.class, false);
        String filePathToAddMetaData = getMediatorParameter(messageContext, AzureConstants.FILE_PATH_TO_ADD_META_DATA, String.class, false);
        String metadata = getMediatorParameter(messageContext, AzureConstants.METADATA, String.class, false);
        Integer timeout = getMediatorParameter(messageContext, AzureConstants.TIMEOUT, Integer.class , true);

        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();

        try {
            AzureStorageConnectionHandler azureStorageConnectionHandler = (AzureStorageConnectionHandler) handler.getConnection(AzureConstants.CONNECTOR_NAME, connectionName);
            DataLakeServiceClient dataLakeServiceClient = azureStorageConnectionHandler.getDataLakeServiceClient();
            DataLakeFileClient dataLakeFileClient = dataLakeServiceClient.getFileSystemClient(fileSystemName).getFileClient(filePathToAddMetaData);

            HashMap<String, String> metadataMap = new HashMap<>();

            String metadataString = metadata != null ? metadata : "";

            Utils.addDataToMapFromJsonString(metadataString , metadataMap);

            Response<?> response = dataLakeFileClient.setMetadataWithResponse(
                    metadataMap,
                    null,
                    timeout != null ? Duration.ofSeconds(timeout.longValue()) : null,
                    null
            );


            if(response.getStatusCode() == 200){
                handleConnectorResponse(messageContext, responseVariable, overwriteBody, true, null, null);
            }else{
                handleConnectorResponse(messageContext, responseVariable, overwriteBody, false, null, null);
            }

        } catch (DataLakeStorageException e) {

            handleConnectorException(Error.DATA_LAKE_STORAGE_GEN2_ERROR, messageContext, e);

        } catch (ConnectException e) {

            handleConnectorException(Error.CONNECTION_ERROR, messageContext, e);

        } catch (JsonSyntaxException e) {

            handleConnectorException(Error.INVALID_JSON, messageContext, e);

        } catch (Exception e) {

            handleConnectorException(Error.GENERAL_ERROR, messageContext, e);

        }
    }


}
