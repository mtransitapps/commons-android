package org.mtransit.android.commons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        assertNotNull(result)
        assertTrue(result.isEmpty())
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
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(
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
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(
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
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(
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
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png",
            result.first()
        )
    }

    @Test
    fun extractImagesUrlsMultipleWithOtherTags() {
        // Arrange
        val fromUrl = URI.create("https://exo.quebec/rss?projection=1568").toURL()
        val textHTML =
            " <img width=\"100%\" src=\"https://exo.quebec/Media/Default/images/section7/Nouvelles/2022/chrono/image-1.jpg\" alt=\"\" />" +
                    "</li> <li> <p>Naviguez pour trouver votre arr&ecirc;t.</p> <div class=\"block-images--container\">" +
                    "<img width=\"100%\" src=\"https://exo.quebec/Media/Default/images/section7/Nouvelles/2022/chrono/image-2.jpg\" alt=\"\" /> " +
                    "<img width=\"100%\" src=\"https://exo.quebec/Media/Default/images/section7/Nouvelles/2022/chrono/image-3.jpg\" alt=\"\" /> " +
                    "<img width=\"100%\" src=\"https://exo.quebec/Media/Default/images/section7/Nouvelles/2022/chrono/image-4.jpg\" alt=\"\" />"
        // Act
        val result = HtmlUtils.extractImagesUrls(fromUrl, textHTML)
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(
            "https://exo.quebec/Media/Default/images/section7/Nouvelles/2022/chrono/image-1.jpg",
            result[0]
        )
        assertEquals(
            "https://exo.quebec/Media/Default/images/section7/Nouvelles/2022/chrono/image-2.jpg",
            result[1]
        )
        assertEquals(
            "https://exo.quebec/Media/Default/images/section7/Nouvelles/2022/chrono/image-3.jpg",
            result[2]
        )
        assertEquals(
            "https://exo.quebec/Media/Default/images/section7/Nouvelles/2022/chrono/image-4.jpg",
            result[3]
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
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png",
            result.first()
        )
        assertEquals(
            "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-02.png",
            result[1]
        )
    }

    @Test
    fun extractImagesUrlsDataImg() {
        // Arrange
        val fromUrl = URI.create("https://exo.quebec/rss?projection=1568").toURL()
        val textHTML =
            "Before" +
                    "<img width=\"100%\" src=\"img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA88AAAGGCAYAAABFQ7NOPumzhG4J5DdeOIArREpFt7F1DD/6V/tESsW7i+mpBe2dCP5MxyePlH6I1A6R7g40eACYohvCZwvEvm95RM0OHvGxSt0WZpYSVZFZJlIRF86Sgg7cXKy2u/NAb8mnNtbVMnOnqGMTA2zeNvgQ/34eEXBa+AaHhy6Rcci6jYdIV1UTcX8T1RuE9KkRvCFaita0RNTYPBahtQXVNPa0B5ZS1KK2o4hlajoqoBJXxvtYpa5JdUIjufY3B2IXIKy1BUVonysmq0NDWjraERQ11dmOdYNzdOGxvE/Hg/NpancHJ9GpfW2vDgVDVeOlrPOakQVk7+cPEOh6tvBDz8oxAWk8rfXNrUpSEtPUeV/iur6nhMjSjl++bkFHAeKOb5kYuOtiYsL4zg2PoUVueHsMCxdnFqCLPj3QT5Vq4bwQnOXwsTg1iRPvrDvVicHMbx1Vn6PTM4xeeWOObPDHVjaXyI4/oox/VuSAtD6cIg/fpPcDw+ujTFeaOXUN3OOWKU4/UkFsb6ua4bl06u4TL9pyML45oSPjfSrcJq85xjl/m+k33tWgI0P9bH4+jHWF+bRsAXx+VmaoNGwse5bqCtnnDPOVRKhnpadJ4+wflDnpebqf0tNdymDm+88mXBsEuXLsHZ2YnnTgBCQoJ4rcn1JNHbAARznfQtFrVsby9p6+gCDzcxZ3h5uCDA1w0xkQEoKUzFzGQf3rh/Db/71cf4H//pjwYQfjTarID8KDSLPbpOotB/r5Hn337xY3R3t/EYOF7F8fwjQEvbKvlfFLO379ihCt8Ctu5evnATE5DWyC7HG841KiLGpbs7xxUeswKxo0Sbt4S9OG7RtgBZ1sljBeVNcJbXCBhLurcCsoDvV57XdfK/bKvwzG1luWkGsOb4RsAXc5bMH46Tbt5+8PD11+OXKLpEzj28PHj8Lsin//XodSrw3PMYnh/bY/tXsi14rsD/C1Ykjq3apjxwAAAAAElFTkSuQmCC\" alt=\"\" />" +
                    "after"
        // Act
        val result = HtmlUtils.extractImagesUrls(fromUrl, textHTML)
        // Assert
        assertNotNull(result)
        assertTrue(result.isEmpty())
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
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
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
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
            "Before after",
            result
        )
    }

    @Test
    fun replaceTagWithUrl() {
        val from = "https://exo.quebec/rss?projection=1568"
        // Arrange
        val textHTML =
            "Before " +
                    "<img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\" /> " +
                    "after"
        // Act
        val result = HtmlUtils.replaceImgTagWithUrlLink(from, textHTML)
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
            "Before" +
                    "<BR/>" +
                    "<A HREF=\"https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\">" +
                    "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png" +
                    "</A>" +
                    "<BR/>" +
                    "after",
            result
        )
    }

    @Test
    fun replaceTagWithUrlDataImage() {
        val from = "https://exo.quebec/rss?projection=1568"
        // Arrange
        val textHTML =
            "Before" +
                    "<img width=\"100%\" src=\"img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA88AAAGGCAYAAABFQ7NOPumzhG4J5DdeOIArREpFt7F1DD/6V/tESsW7i+mpBe2dCP5MxyePlH6I1A6R7g40eACYohvCZwvEvm95RM0OHvGxSt0WZpYSVZFZJlIRF86Sgg7cXKy2u/NAb8mnNtbVMnOnqGMTA2zeNvgQ/34eEXBa+AaHhy6Rcci6jYdIV1UTcX8T1RuE9KkRvCFaita0RNTYPBahtQXVNPa0B5ZS1KK2o4hlajoqoBJXxvtYpa5JdUIjufY3B2IXIKy1BUVonysmq0NDWjraERQ11dmOdYNzdOGxvE/Hg/NpancHJ9GpfW2vDgVDVeOlrPOakQVk7+cPEOh6tvBDz8oxAWk8rfXNrUpSEtPUeV/iur6nhMjSjl++bkFHAeKOb5kYuOtiYsL4zg2PoUVueHsMCxdnFqCLPj3QT5Vq4bwQnOXwsTg1iRPvrDvVicHMbx1Vn6PTM4xeeWOObPDHVjaXyI4/oox/VuSAtD6cIg/fpPcDw+ujTFeaOXUN3OOWKU4/UkFsb6ua4bl06u4TL9pyML45oSPjfSrcJq85xjl/m+k33tWgI0P9bH4+jHWF+bRsAXx+VmaoNGwse5bqCtnnDPOVRKhnpadJ4+wflDnpebqf0tNdymDm+88mXBsEuXLsHZ2YnnTgBCQoJ4rcn1JNHbAARznfQtFrVsby9p6+gCDzcxZ3h5uCDA1w0xkQEoKUzFzGQf3rh/Db/71cf4H//pjwYQfjTarID8KDSLPbpOotB/r5Hn337xY3R3t/EYOF7F8fwjQEvbKvlfFLO379ihCt8Ctu5evnATE5DWyC7HG841KiLGpbs7xxUeswKxo0Sbt4S9OG7RtgBZ1sljBeVNcJbXCBhLurcCsoDvV57XdfK/bKvwzG1luWkGsOb4RsAXc5bMH46Tbt5+8PD11+OXKLpEzj28PHj8Lsin//XodSrw3PMYnh/bY/tXsi14rsD/C1Ykjq3apjxwAAAAAElFTkSuQmCC\" alt=\"\" />" +
                    "after"
        // Act
        val result = HtmlUtils.replaceImgTagWithUrlLink(from, textHTML)
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
            "Before" +
                    "<BR/>" +
                    "after",
            result
        )
    }

    @Test
    fun replaceTagWithUrlNoSpace() {
        val from = "https://exo.quebec/rss?projection=1568"
        // Arrange
        val textHTML =
            "Before" +
                    "<img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\" />" +
                    "after"
        // Act
        val result = HtmlUtils.replaceImgTagWithUrlLink(from, textHTML)
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
            "Before" +
                    "<BR/>" +
                    "<A HREF=\"https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\">" +
                    "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png" +
                    "</A>" +
                    "<BR/>" +
                    "after",
            result
        )
    }

    @Test
    fun replaceTagWithUrlMultiple() {
        val from = "https://exo.quebec/rss?projection=1568"
        // Arrange
        val textHTML =
            "Before " +
                    "<img width=\"100%\" src=\"/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\" /> " +
                    "middle " +
                    "<img width=\"100%\" src=\"https://exo.quebec/Media/22222222.png\" /> " +
                    "after."
        // Act
        val result = HtmlUtils.replaceImgTagWithUrlLink(from, textHTML)
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
            "Before" +
                    "<BR/>" +
                    "<A HREF=\"https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png\">" +
                    "https://exo.quebec/Media/Default/pdf/Avis/2020/Avis_terminus_LaPrairie_plan_1-01-01.png" +
                    "</A>" +
                    "<BR/>" +
                    "middle" +
                    "<BR/>" +
                    "<A HREF=\"https://exo.quebec/Media/22222222.png\">" +
                    "https://exo.quebec/Media/22222222.png" +
                    "</A>" +
                    "<BR/>" +
                    "after.",
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
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
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
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
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
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals(
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
        assertNotNull(result)
        assertTrue(result.isNotBlank())
        assertEquals("Before after", result)
    }

    @Test
    fun removeStyle2() {
        val textHTML = "Before <style type=\"text/css\">hr.divider {\n" +
                "\n" +
                "        border-top: 1px solid #d2d2d2;\n" +
                "        }\n" +
                "        #Walkley10613 table {\n" +
                "        table-layout: auto;\n" +
                "        width: 100%;\n" +
                "        }\n" +
                "\n" +
                "        #Walkley10613 a.map-link {\n" +
                "            height: 1rem !important;\n" +
                "            background-size: contain !important;\n" +
                "        line-height: 1rem;\n" +
                "        min-height: 1rem;\n" +
                "            padding-left: 1.25rem;\n" +
                "\n" +
                "        }\n" +
                "        </style>\n" +
                "after"

        val result = HtmlUtils.removeStyle(textHTML)

        assertEquals("Before after", result)
    }
}