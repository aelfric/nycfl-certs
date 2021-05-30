package org.nycfl.certificates;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Path("/s3")
@RolesAllowed({"basicuser","superuser"})
public class S3SyncResource extends S3Resource {
    @Inject
    S3Client s3;

    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("superuser")
    public Response uploadFile(@MultipartForm MultipartBody formData) {

        if (formData.fileName == null || formData.fileName.isEmpty()) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        PutObjectResponse putResponse = s3.putObject(
                buildPublicPutRequest(formData),
                RequestBody.fromFile(uploadToTemp(formData.file)));
        if (putResponse != null) {
            return Response.ok().status(Status.CREATED).build();
        } else {
            return Response.serverError().build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PublicListing> listFiles() {
        ListObjectsRequest listRequest = ListObjectsRequest.builder().bucket(bucketName).build();
        //HEAD S3 objects to get metadata
        return s3.listObjects(listRequest)
            .contents()
            .stream()
            .sorted(Comparator.comparing(S3Object::lastModified).reversed())
            .map(o->getPublicListing(o.key()))
            .collect(Collectors.toList());
    }

    private PublicListing getPublicListing(String objectName) {
        S3Utilities utilities = s3.utilities();
        GetUrlRequest request = GetUrlRequest.builder()
            .bucket(bucketName)
            .key(objectName)
            .build();
        URL url = utilities.getUrl(request);
        return new PublicListing(url, objectName);
    }

    public static class PublicListing {
        public final URL url;
        public final String objectName;

        public PublicListing(URL url, String objectName) {
            this.url = url;
            this.objectName = objectName;
        }
    }

}