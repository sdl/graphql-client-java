package com.sdl.web.pca.client.contentmodel;

public interface IPagination {
    int First = 0;
    String After = null;

    public int getFirst();
    public void setFirst(int first);

    public String getAfter();
    public void setAfter(String after);
}