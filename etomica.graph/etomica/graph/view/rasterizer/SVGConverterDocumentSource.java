package etomica.graph.view.rasterizer;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.svg.SVGDocument;


public class SVGConverterDocumentSource implements SVGConverterSource {

  private SVGDocument svgDocument;

  public SVGConverterDocumentSource(SVGDocument document) {
    
    svgDocument = document;
  }
  
  public String getName() {

    return svgDocument.getTitle();
  }

  public String getURI() {

    return svgDocument.getDocumentURI();
  }

  public boolean isReadable() {

    return false;
  }

  public boolean isSameAs(String srcStr) {

    // TODO Auto-generated method stub
    return false;
  }

  public InputStream openStream() throws IOException {

    // TODO Auto-generated method stub
    return null;
  }
}
