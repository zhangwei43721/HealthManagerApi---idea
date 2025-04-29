package com.rabbiter.healthsys.utils;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Component
public class R2Utils {

    @Value("${R2.access-key-id}")
    String accessKeyId;

    @Value("${R2.secret-access-key}")
    String secretAccessKey;

    @Value("${R2.end-point}")
    String endpoint;

    @Value("${R2.bucket-name}")
    String bucketName;

    @Value("${R2.public-domain}")
    String publicDomain;

    public String uploadFile(MultipartFile file) throws IOException {
        S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();

        String key = file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        PutObjectResponse response = s3.putObject(
                putObjectRequest,
                RequestBody.fromBytes(file.getBytes())
        );

        log.info("File uploaded successfully to R2: eTag={}, key={}", response.eTag(), key);

        s3.close();

        String url = publicDomain + key;
        log.info("File accessible at: {}", url);
        return url;
    }
} 