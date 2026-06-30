package com.tonepilot.domain.storage;

import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;







import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {

    StoredFile storeImage(MultipartFile file, String folder);

    String writeTextFile(String folder, String fileName, String content);

    String writeBinaryFile(String folder, String fileName, byte[] bytes, String contentType);

    StoredObject readObject(String fileUrl);

    String readAsDataUrl(String fileUrl);

    String slug(String value);
}
