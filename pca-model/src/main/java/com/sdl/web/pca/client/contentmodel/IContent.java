package com.sdl.web.pca.client.contentmodel;

interface IContent
{
    String id = null;
    String type = null;


    String getId();
    void setId(String id);

    String getType();
    void setType(String type);
}