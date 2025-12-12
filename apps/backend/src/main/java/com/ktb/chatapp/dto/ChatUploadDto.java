package com.ktb.chatapp.dto;

import com.ktb.chatapp.model.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class ChatUploadDto {
    File file;
}
