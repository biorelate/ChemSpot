
/* First created by JCasGen Wed Mar 16 10:14:06 CET 2011 */
package org.u_compare.shared.label.penn.coindex;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;

/** Permanent Predictable Ambiguity<BR></BR>
 * Updated by JCasGen Tue Mar 06 16:28:15 CET 2012
 * @generated */
public class PPA_Type extends NullElementLabel_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (PPA_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = PPA_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new PPA(addr, PPA_Type.this);
  			   PPA_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new PPA(addr, PPA_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = PPA.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.u_compare.shared.label.penn.coindex.PPA");



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public PPA_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

  }
}



    