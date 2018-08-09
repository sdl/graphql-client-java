package com.sdl.web.pca.client.contentmodel;

/// <summary>
/// Represents a filter for a publication query.
/// </summary>
class InputPublicationFilter
{
    private ContentFilterQueryType queryType;
    private String value;
    private CustomMetaValueType valueType;

     public ContentFilterQueryType getQueryType()
     {
         return queryType;
     }
     public void setQueryType(ContentFilterQueryType queryType)
     {
         this.queryType = queryType;
     }

     public String getValue()
     {
         return value;
     }
     public void setValue(String value)
     {
         this.value = value;
     }

     public CustomMetaValueType getValueType()
     {
         return valueType;
     }
     public void setValueType(CustomMetaValueType valueType)
     {
         this.valueType = valueType;
     }
}