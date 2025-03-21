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
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import com.azure.storage.file.datalake.models.PublicAccessType;
import org.apache.synapse.MessageContext;
import org.apache.synapse.util.InlineExpressionUtil;
import org.json.JSONObject;
import org.wso2.carbon.connector.connection.AzureStorageConnectionHandler;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.util.AbstractAzureMediator;
import org.wso2.carbon.connector.util.AzureConstants;
import org.wso2.carbon.connector.util.Error;
import org.wso2.carbon.connector.util.Utils;

import java.time.Duration;
import java.util.HashMap;

/**
 * Handles the creation of a filesystem in Azure Data Lake.
 */
public class CreateFileSystem extends AbstractAzureMediator {

    @Override
    public void execute(MessageContext messageContext, String responseVariable, Boolean overwriteBody) {

        String connectionName =
                getProperty(messageContext, AzureConstants.CONNECTION_NAME, String.class, false);
        String fileSystemName =
                getMediatorParameter(messageContext, AzureConstants.FILE_SYSTEM_NAME, String.class, false);
        Integer timeout = getMediatorParameter(messageContext, AzureConstants.TIMEOUT, Integer.class, true);
        String metadata = getMediatorParameter(messageContext, AzureConstants.METADATA, String.class, true);
        String accessType = getMediatorParameter(messageContext, AzureConstants.ACCESS_TYPE, String.class, true);

        PublicAccessType publicAccessType = null;

        switch (accessType) {
            case "BLOB":
                publicAccessType = PublicAccessType.BLOB;
                break;
            case "CONTAINER":
                publicAccessType = PublicAccessType.CONTAINER;
                break;
        }

        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
        try {
            AzureStorageConnectionHandler azureStorageConnectionHandler =
                    (AzureStorageConnectionHandler) handler.getConnection(AzureConstants.CONNECTOR_NAME,
                            connectionName);
            DataLakeServiceClient dataLakeServiceClient = azureStorageConnectionHandler.getDataLakeServiceClient();
            DataLakeFileSystemClient dataLakeFileSystemClient =
                    dataLakeServiceClient.getFileSystemClient(fileSystemName != null ? fileSystemName : "");
            metadata = InlineExpressionUtil.processInLineSynapseExpressionTemplate(messageContext, metadata);

            HashMap<String, String> metadataMap = new HashMap<>();
            if (metadata != null) {
                Utils.addDataToMapFromJsonString(metadata, metadataMap);
            }
            Response<?> response = dataLakeFileSystemClient.createIfNotExistsWithResponse(
                    metadata != null ? metadataMap : null, publicAccessType,
                    timeout != null ? Duration.ofSeconds(timeout.longValue()) : null, null);

            if (response.getStatusCode() == 201) {
                JSONObject responseObject = new JSONObject();
                responseObject.put("success", true);
                responseObject.put("message", "Successfully created the filesystem");
                responseObject.put("fileSystemName", fileSystemName);

                handleConnectorResponse(messageContext, responseVariable, overwriteBody, responseObject, null,
                        null);
            } else {

                handleConnectorException(Error.FILE_ALREADY_EXISTS_ERROR, messageContext);
            }

        } catch (ConnectException e) {
            handleConnectorException(Error.CONNECTION_ERROR, messageContext, e);
        } catch (DataLakeStorageException e) {
            handleConnectorException(Error.DATA_LAKE_STORAGE_GEN2_ERROR, messageContext, e);
        } catch (Exception e) {
            handleConnectorException(Error.GENERAL_ERROR, messageContext, e);
        }

    }
}
