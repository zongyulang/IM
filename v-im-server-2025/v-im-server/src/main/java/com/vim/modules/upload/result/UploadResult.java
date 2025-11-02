package com.vim.modules.upload.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UploadResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String url;
    private String fileName;
    private String originalFilename;


}
