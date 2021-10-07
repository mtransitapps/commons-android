package org.mtransit.android.commons

import org.junit.Assert
import org.junit.Test
import java.net.URI

class HtmlUtilsTest {

    @Test
    fun extractImagesUrlsNotAnImage() {
        // Arrange
        val from = "https://exo.quebec/rss?projection=1568"
        val textHTML =
            "<img width=\"100%\" src=\"https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.pdf\" />"
        // Act
        val result = HtmlUtils.extractImagesUrls(from, textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun extractImagesUrlsWithDomain() {
        // Arrange
        val from = "https://exo.quebec/rss?projection=1568"
        val textHTML =
            "<img width=\"100%\" src=\"https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\" />"
        // Act
        val result = HtmlUtils.extractImagesUrls(from, textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotEmpty())
        Assert.assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png",
            result.first()
        )
    }

    @Test
    fun extractImagesUrlsWithoutDomain() {
        // Arrange
        val from = "https://exo.quebec/rss?projection=1568"
        val textHTML =
            "<img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\" />"
        // Act
        val result = HtmlUtils.extractImagesUrls(from, textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotEmpty())
        Assert.assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png",
            result.first()
        )
    }

    @Test
    fun extractImagesUrlsWithoutDomainUri() {
        // Arrange
        val fromUri = URI.create("https://exo.quebec/rss?projection=1568")
        val textHTML =
            "<img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\" />"
        // Act
        val result = HtmlUtils.extractImagesUrls(fromUri, textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotEmpty())
        Assert.assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png",
            result.first()
        )
    }

    @Test
    fun extractImagesUrlsWithoutDomainUrl() {
        // Arrange
        val fromUrl = URI.create("https://exo.quebec/rss?projection=1568").toURL()
        val textHTML =
            "<img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\" />"
        // Act
        val result = HtmlUtils.extractImagesUrls(fromUrl, textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotEmpty())
        Assert.assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png",
            result.first()
        )
    }

    @Test
    fun extractImagesUrlsMultiple() {
        // Arrange
        val from = "https://exo.quebec/rss?projection=1568"
        val textHTML = "<span id=\"desc_4\" class=\"item_desc\">" +
                "<p>Du 8 juin au 22 août, en raison de travaux de reconstruction du système de drainage, exo sera contraint de fermer temporairement une partie du stationnement et de modifier des quais d’embarquement.</p>\n" +
                "<p>Les travaux se dérouleront en 2 phases. La phase 1, durant laquelle la petite boucle d’autobus sera fermée, aura lieu du 8 au 12 juin. Les quais d’embarquement seront relocalisés dans la grande boucle d’autobus et sur l’avenue du Golf, selon le schéma ci-dessous.</p>\n" +
                "<p>La phase 2, durant laquelle une grande partie du stationnement sera fermée, aura lieu du 8 juin au 22 août 2020.</p>\n" +
                "<p>&nbsp;</p>\n" +
                "<p><strong>Phase 1 : 8 au 12 juin 2020&nbsp;</strong></p>\n" +
                "<p><img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\"></p>\n" +
                "<p></p>\n" +
                "<p><strong>Phase 2 : 8 juin au 22 août 2020</strong></p>\n" +
                "<p><img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-02.png\"></p>\n" +
                "<p>&nbsp;</p>\n" +
                "<p>Nous désirons rappeler à la clientèle que des places de stationnement sont également disponibles au stationnement du parc des Jésuites, situé à l’angle du boulevard Taschereau et de l’avenue Balmoral. De plus, le Golf Espace Rive-Sud offre quelques espaces supplémentaires dans le secteur à proximité des condos.</p>\n" +
                "<p>&nbsp;</p>\n" +
                "<p>Merci de votre collaboration.</p>" +
                "</span>"
        // Act
        val result = HtmlUtils.extractImagesUrls(from, textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(2, result.size)
        Assert.assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png",
            result.first()
        )
        Assert.assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-02.png",
            result[1]
        )
    }

    @Test
    fun removeImg() {
        // Arrange
        val textHTML =
            "Before " +
                    "<img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\">" +
                    "after"
        // Act
        val result = HtmlUtils.removeImg(textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotBlank())
        Assert.assertEquals(
            "Before after",
            result
        )
    }

    @Test
    fun removeImg2() {
        // Arrange
        val textHTML =
            "Before " +
                    "<img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\" />" +
                    "after"
        // Act
        val result = HtmlUtils.removeImg(textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotBlank())
        Assert.assertEquals(
            "Before after",
            result
        )
    }

    @Test
    fun removeComments() {
        // Arrange
        val textHTML = "I like this <!-- but not this --> and I like this as well."
        // Act
        val result = HtmlUtils.removeComments(textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotBlank())
        Assert.assertEquals(
            "I like this  and I like this as well.",
            result
        )
    }

    @Test
    fun removeCommentsPlus() {
        // Arrange
        val textHTML =
            "I like this one <!-- but not this -->and I like this second<!--but not this--> and I like this third<!--\n" +
                    "\t\n" +
                    "but not this\n" +
                    "--> and I like this fourth."
        // Act
        val result = HtmlUtils.removeComments(textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotBlank())
        Assert.assertEquals(
            "I like this one and I like this second and I like this third and I like this fourth.",
            result
        )
    }

    @Test
    fun removeCommentsComplex() {
        // Arrange
        val textHTML = "<style type=\"text/css\"><!--\n" +
                "\t\n" +
                ".encadre {\n" +
                "\tbackground-color: white;\n" +
                "\tpadding: 20px;\n" +
                "  width: 100%;\n" +
                "  border-radius: 10px;\n" +
                "}\n" +
                "\n" +
                "/* Clear floats after the columns */\n" +
                ".row:after {\n" +
                "  clear: both;\n" +
                "}\n" +
                "\n" +
                "@media only screen and (max-width: 600px) {\n" +
                "\n" +
                "}\n" +
                "--></style>"
        // Act
        val result = HtmlUtils.removeComments(textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotBlank())
        Assert.assertEquals(
            "<style type=\"text/css\"></style>",
            result
        )
    }

    @Test
    fun removeStyle() {
        // Arrange
        val textHTML = "Before <style type=\"text/css\"><!--\n" +
                "\t\n" +
                ".encadre {\n" +
                "\tbackground-color: white;\n" +
                "\tpadding: 20px;\n" +
                "  width: 100%;\n" +
                "  border-radius: 10px;\n" +
                "}\n" +
                "\n" +
                "/* Clear floats after the columns */\n" +
                ".row:after {\n" +
                "  clear: both;\n" +
                "}\n" +
                "\n" +
                "@media only screen and (max-width: 600px) {\n" +
                "\n" +
                "}\n" +
                "--></style>after"
        // Act
        val result = HtmlUtils.removeStyle(textHTML)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertTrue(result.isNotBlank())
        Assert.assertEquals(
            "Before after",
            result
        )
    }
}