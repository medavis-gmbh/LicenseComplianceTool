/*-
 * #%L
 * CoMiC - Component Manifest Creator
 * %%
 * Copyright (C) 2022 medavis GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.medavis.license.comic.core.creator;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static com.lowagie.text.Rectangle.BOX;

import de.medavis.license.comic.core.license.License;
import de.medavis.license.comic.core.list.ComponentData;

class PDFOutputter implements Outputter {

    private static final Font FONT_DEFAULT = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
    private static final Font FONT_HYPERLINK = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.UNDERLINE, Color.BLUE);

    @Override
    public void output(List<ComponentData> data, Path outputFile) {
        try (FileOutputStream outputFileStream = new FileOutputStream(outputFile.toFile());
                Document document = new Document(PageSize.A4)) {

            PdfWriter.getInstance(document, outputFileStream);

            document.open();

            float[] columnSizes = new float[]{0.4f, 0.2f, 0.4f};
            PdfPTable table = new PdfPTable(columnSizes);
            table.getDefaultCell().setBorder(BOX);
            table.getDefaultCell().setPadding(5);
            table.setHorizontalAlignment(0);
            table.setTotalWidth(document.getPageSize().getWidth() - 72);
            table.setLockedWidth(true);

            table.addCell(new Phrase("Component", FONT_HEADER));
            table.addCell(new Phrase("Version", FONT_HEADER));
            table.addCell(new Phrase("Licenses", FONT_HEADER));
            // Do not set header row if there is no data because this causes OpenPDF to fail with "PDF has no pages"
            if (!data.isEmpty()) {
                table.setHeaderRows(1);
            }

            data.forEach(component -> {
                table.addCell(renderTextWithOptionalUrl(component.name(), component.url()));
                table.addCell(new Phrase(component.version(), FONT_DEFAULT));

                Paragraph licenseContent = new Paragraph();

                for (Iterator<License> iterator = component.licenses().iterator(); iterator.hasNext(); ) {
                    License licenseData = iterator.next();
                    licenseContent.add(renderTextWithOptionalUrl(licenseData.name(), licenseData.url()));
                    if (iterator.hasNext()) {
                        licenseContent.add(Chunk.NEWLINE);
                    }
                }
                table.addCell(licenseContent);
            });
            document.add(table);


        } catch (IOException e) {
            throw new IllegalStateException("Cannot write components as PDF to file " + outputFile, e);
        }

    }

    private Phrase renderTextWithOptionalUrl(String text, String url) {
        if (url != null) {
            Chunk urlChunk;
            urlChunk = new Chunk(text);
            urlChunk.setAnchor(url);
            urlChunk.setFont(FONT_HYPERLINK);
            return new Paragraph(urlChunk);
        } else {
            return new Phrase(text, FONT_DEFAULT);
        }
    }
}
