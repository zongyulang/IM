package com.vim.tio.service;

public interface ConnStatusService {

    void  setConnStatus(String userId, boolean status);

    boolean getConnStatus(String userId);

}
