package org.nycfl.certificates.requests;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.NonNull;
import org.nycfl.certificates.Tournament;

import java.time.LocalDate;

public record TournamentInput(
    @NotNull
    String host,
    @NotNull
    @JsonbDateFormat(value = "yyyy-MM-dd")
    LocalDate date,
    @NotNull
    String name,
    @Schema(nullable = true) String logoUrl,
    @Schema(nullable = true) String slideBackgroundUrl,
    @Schema(nullable = true) String slideAccentColor,
    @Schema(nullable = true) String slideSecondaryAccentColor,
    @Schema(nullable = true) String slidePrimaryColor,
    @Schema(nullable = true) String slideOverlayColor,
    @Schema(nullable = true) String signature,
    @Schema(nullable = true) String signatureTitle,
    @Schema(nullable = true) String certificateHeadline,
    @Schema(nullable = true) String styleOverrides,
    @Schema(nullable = true) String line1,
    @Schema(nullable = true) String line2
) {
    public static @NonNull TournamentInput from(Tournament source) {
        return new TournamentInput(
            source.getHost(),
            source.getDate(),
            source.getName(),
            source.getLogoUrl(),
            source.getSlideBackgroundUrl(),
            source.getSlideAccentColor(),
            source.getSlideSecondaryAccentColor(),
            source.getSlidePrimaryColor(),
            source.getSlideOverlayColor(),
            source.getSignature(),
            source.getSignatureTitle(),
            source.getCertificateHeadline(),
            source.getStyleOverrides(),
            source.getLine1(),
            source.getLine2()
        );
    }
}