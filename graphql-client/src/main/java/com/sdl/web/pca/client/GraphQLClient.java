package com.sdl.web.pca.client;

import com.sdl.web.pca.client.exception.GraphQLClientException;
import com.sdl.web.pca.client.exception.UnauthorizedException;
import com.sdl.web.pca.client.request.GraphQLRequest;

/**
 * This interface enables java clients to connect to the GraphQL Service
 */
public interface GraphQLClient {

    /**
     * This method can be used to execute the graphQL queries.
     *
     * @param jsonEntity graphql query which needs to be executed against the graphql server
     * @param timeout    specify the timeout period in milliseconds
     * @return The GraphQL JSON string response with data and errors if any.
     * @throws GraphQLClientException Thrown when request to GraphQL Service fail
     */
    String execute(String jsonEntity, int timeout) throws UnauthorizedException, GraphQLClientException;

    /**
     * This method can be used to execute the graphQL queries with no timeout.
     *
     * @param jsonEntity graphql query which needs to be executed against the graphql server
     * @return The GraphQL JSON string response with data and errors if any.
     * @throws GraphQLClientException Thrown when request to GraphQL Service fail
     */
    String execute(String jsonEntity) throws UnauthorizedException, GraphQLClientException;

    /**
     * This method can be used to execute the GraphQL queries against the GraphQL server for the GraphQLRequest parameter
     *
     * @param request GraphQLRequest object which holds the information to execute the query.
     * @return The GraphQL JSON string response with data and errors if any.
     * @throws GraphQLClientException Thrown when request to GraphQL Service fail
     */
    String execute(GraphQLRequest request) throws UnauthorizedException, GraphQLClientException;

    /**
     * Adds default header to request.
     * @param header HTTP Header name
     * @param value HTTP Header value
     */
    void addDefaultHeader(String header, String value);
}
