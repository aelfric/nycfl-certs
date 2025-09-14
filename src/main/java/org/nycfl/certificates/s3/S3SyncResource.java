package org.nycfl.certificates.s3;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

@Path("/s3")
@RolesAllowed({"basicuser", "superuser"})
public class S3SyncResource extends S3Resource {
    private static final Logger LOG = Logger.getLogger(S3SyncResource.class);
    private final S3Client s3;

    @ConfigProperty(name = "cloudfront.host")
    String cloudfrontHost;

    @Inject
    public S3SyncResource(S3Client s3) {
        this.s3 = s3;
    }

    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("superuser")
    public Response uploadFile(@RestForm("file") FileUpload file) {

        if (file == null || file.fileName().isEmpty()) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        try (var is = Files.newInputStream(file.uploadedFile())) {
            PutObjectResponse putResponse = s3.putObject(
                buildPublicPutRequest(file.fileName()),
                RequestBody.fromFile(uploadToTemp(is)));
            if (putResponse != null) {
                return Response.ok().status(Status.CREATED).build();
            } else {
                return Response.serverError().build();
            }

        } catch (IOException _) {
            return Response.serverError().build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PublicListing> listFiles() {
        ListObjectsRequest
            listRequest =
            ListObjectsRequest.builder().bucket(bucketName).build();
        //HEAD S3 objects to get metadata
        return s3.listObjects(listRequest)
            .contents()
            .stream()
            .sorted(Comparator.comparing(S3Object::lastModified).reversed())
            .map(o -> getPublicListing(o.key()))
            .toList();
    }

    private PublicListing getPublicListing(String objectName) {
        try {
            return new PublicListing(
                    new URI(
                            "https",
                            cloudfrontHost,
                            "/" + objectName,
                            null
                    ).toURL(),
                objectName
            );
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.error("Could not construct resource URL", e);
            throw new InternalServerErrorException("Could not construct resource URL", e);
        }
    }

    public record PublicListing(URL url, String objectName) {
    }

}