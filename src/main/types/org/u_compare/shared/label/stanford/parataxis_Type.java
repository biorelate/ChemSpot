
/* First created by JCasGen Wed Mar 16 10:14:08 CET 2011 */
package org.u_compare.shared.label.stanford;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;

/** Stanford Dependency: parataxis
 * Updated by JCasGen Tue Mar 06 16:28:18 CET 2012
 * @generated */
public class parataxis_Type extends StanfordDependencyLabel_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (parataxis_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = parataxis_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new parataxis(addr, parataxis_Type.this);
  			   parataxis_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new parataxis(addr, parataxis_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = parataxis.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.u_compare.shared.label.stanford.parataxis");



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public parataxis_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

  }
}



    