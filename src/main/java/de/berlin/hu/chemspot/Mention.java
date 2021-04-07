/*
 * Copyright (c) 2012. Humboldt-Universität zu Berlin, Dept. of Computer Science and Dept.
 * of Wissensmanagement in der Bioinformatik
 * -------------------------------
 *
 * THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS COMMON PUBLIC
 * LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM
 * CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 *
 * http://www.opensource.org/licenses/cpl1.0
 */

package de.berlin.hu.chemspot;

import de.berlin.hu.util.Constants;
import de.berlin.hu.util.Constants.ChemicalID;
import de.berlin.hu.util.Constants.ChemicalType;

import org.apache.uima.jcas.tcas.Annotation;
import org.u_compare.shared.semantic.NamedEntity;

public class Mention implements Comparable<Object> {
	private int start;
	private int end;
	private String text;
	private String[] ids;
	private String source;
	private String documentText;
	private ChemicalType type;

    /**
     * Represents a chemical entity found in a text.
     * @param start position of the start character of an annotation
     * @param end position of the end character of an annotation (exclusive)
     * @param text covered text
     * @param ids a string representation of an array of identifiers of the form: [0] CHID, [1] CHEB, [2] CAS, [3] PUBC, [4] PUBS, [5] INCH, [6] DRUG, [7] HMBD, [8] KEGG, [9] KEGD, [10] MESH
     * @param source indicates whether found by the CRF, the dictionary or taken from goldstandard
     */
	public Mention(int start, int end, String text, String ids, String source, String documentText, ChemicalType type) {
		this.start = start;
		this.end = end;
		this.text = text;
		this.ids = new String[ChemicalID.values().length];
        if (ids != null) {
            String tempIds = ids;
            if (tempIds.startsWith("[")) tempIds = tempIds.substring(1);
            if (tempIds.endsWith("]")) tempIds = tempIds.substring(0, tempIds.length() - 1);
            int i = 0;
            for (String id : tempIds.split(", |,$|^,")) {
            	if (i >= this.ids.length) break;
            	setId(ChemicalID.values()[i++], id.trim());
            }
        }
       	
        this.documentText = documentText;
        setSource(source);
        setType(type);
	}
	
	public Mention(int start, int end, String text, String ids, String source, String documentText) {
		this(start, end, text, ids, source, documentText, null); 
	}

    public Mention(int start, int end, String text) {
    	this(start, end, text, null, null, null);
   	}

    public Mention(int start, int end) {
    	this(start, end, null);
    }

    public Mention(NamedEntity entity) {
        this(entity.getBegin(), entity.getEnd(), entity.getCoveredText(), entity.getId(), entity.getSource(), entity.getCAS().getDocumentText(), ChemicalType.fromString(entity.getEntityType()));
    }

    public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public String getText() {
		return text;
	}
	public String[] getIds() {
		return ids;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source == null ? Constants.UNKNOWN : source;
		
		if (getType() == null || ChemicalType.UNKNOWN.equals(getType())) {
			setType(ChemicalType.fromString(source));
		}
	}

    public String getCHID() {
        return getId(ChemicalID.CHID);
    }

    public String getCHEB() {
        return getId(ChemicalID.CHEBI);
    }

    public String getCAS() {
        return getId(ChemicalID.CAS);
    }

    public String getPUBC() {
        return getId(ChemicalID.PUBC);
    }

    public String getPUBS() {
        return getId(ChemicalID.PUBS);
    }

    public String getINCH() {
        return getId(ChemicalID.INCH);
    }

    public String getDRUG() {
        return getId(ChemicalID.DRUG);
    }

    public String getHMBD() {
        return getId(ChemicalID.HMBD);
    }

    public String getKEGG() {
        return getId(ChemicalID.KEGG);
    }

    public String getKEGD() {
        return getId(ChemicalID.KEGD);
    }

    public String getMESH() {
        return getId(ChemicalID.MESH);
    }
    
    public String getFDA() {
        return getId(ChemicalID.FDA);
    }

    public String getFDADate() {
        return getId(ChemicalID.FDA_DATE);
    }

    public String getId(ChemicalID type) {
        String id = null;
        try {
          id = ids[type.ordinal()];
        } catch (ArrayIndexOutOfBoundsException e) {
            //ignore
        }
        return id;
    }
    
    public void setCHID(String id) {
        setId(ChemicalID.CHID, id);
    }

    public void setCHEB(String id) {
        setId(ChemicalID.CHEBI, id);
    }

    public void setCAS(String id) {
        setId(ChemicalID.CAS, id);
    }

    public void setPUBC(String id) {
        setId(ChemicalID.PUBC, id);
    }

    public void setPUBS(String id) {
        setId(ChemicalID.PUBS, id);
    }

    public void setINCH(String id) {
        setId(ChemicalID.INCH, id);
    }

    public void setDRUG(String id) {
        setId(ChemicalID.DRUG, id);
    }

    public void setHMBD(String id) {
        setId(ChemicalID.HMBD, id);
    }

    public void setKEGG(String id) {
        setId(ChemicalID.KEGG, id);
    }

    public void setKEGD(String id) {
        setId(ChemicalID.KEGD, id);
    }

    public void setMESH(String id) {
        setId(ChemicalID.MESH, id);
    }
    
    public void setFDA(String id) {
        setId(ChemicalID.FDA, id);
    }
    
    public void setFDADate(String id) {
        setId(ChemicalID.FDA_DATE, id);
    }
    
    public void setId(ChemicalID type, String id) {
    	if (id != null && (id.isEmpty() || "null".equals(id))) {
    		id = null;
    	}

        if (ChemicalID.CHID.equals(type) && id != null && !id.startsWith(ChemicalID.CHID.toString() + ":")) {
            id = ChemicalID.CHID.toString() + ":" + id;
        }

    	if (ChemicalID.CHEBI.equals(type) && id != null && !id.startsWith(ChemicalID.CHEBI.toString() + ":")) {
    		id = ChemicalID.CHEBI.toString() + ":" + id;
    	}

        if (ChemicalID.CAS.equals(type) && id != null && !id.startsWith(ChemicalID.CAS.toString() + ":")) {
            id = ChemicalID.CAS.toString() + ":" + id;
        }

        if (ChemicalID.PUBC.equals(type) && id != null && !id.startsWith(ChemicalID.PUBC.toString() + ":")) {
            id = ChemicalID.PUBC.toString() + ":" + id;
        }

        if (ChemicalID.PUBS.equals(type) && id != null && !id.startsWith(ChemicalID.PUBS.toString() + ":")) {
            id = ChemicalID.PUBS.toString() + ":" + id;
        }

        if (ChemicalID.INCH.equals(type) && id != null && !id.startsWith(ChemicalID.INCH.toString() + ":")) {
            id = ChemicalID.INCH.toString() + ":" + id;
        }

        if (ChemicalID.DRUG.equals(type) && id != null && !id.startsWith(ChemicalID.DRUG.toString() + ":")) {
            id = ChemicalID.DRUG.toString() + ":" + id;
        }

        if (ChemicalID.HMBD.equals(type) && id != null && !id.startsWith(ChemicalID.HMBD.toString() + ":")) {
            id = ChemicalID.HMBD.toString() + ":" + id;
        }

        if (ChemicalID.KEGG.equals(type) && id != null && !id.startsWith(ChemicalID.KEGG.toString() + ":")) {
            id = ChemicalID.KEGG.toString() + ":" + id;
        }

        if (ChemicalID.KEGD.equals(type) && id != null && !id.startsWith(ChemicalID.KEGD.toString() + ":")) {
            id = ChemicalID.KEGD.toString() + ":" + id;
        }

        if (ChemicalID.MESH.equals(type) && id != null && !id.startsWith(ChemicalID.MESH.toString() + ":")) {
            id = ChemicalID.MESH.toString() + ":" + id;
        }

        try {
          ids[type.ordinal()] = id != null ? id.trim() : null;
        } catch (ArrayIndexOutOfBoundsException e) {
            //ignore
        }
    }
    
    public boolean equals(Object obj) {
    	if (this == obj) {
    		return true;
    	}
    	
    	if (obj == null || !(obj instanceof Mention)) {
    		return false;
    	}
    	
		Mention other = (Mention) obj;
		if (getStart() != other.getStart() || getEnd() != other.getEnd()
				|| (getText() == null && other.getText() != null) || (getText() != null && !getText().equals(other.getText()))) {
			return false;
		}

		return true;
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getStart();
		result = prime * result + getEnd();
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

    @Override
    public String toString() {
        return start + " " + end + " " + text + " " + getCHID();
    }

	public int compareTo(Object o) {
		if (this.equals(o)) {
			return 0;
		}
		
		int otherBegin = 0;
		int otherEnd = 0;
		
		if (o instanceof Mention) {
			Mention other = (Mention) o;
			otherBegin = other.getStart();
			otherEnd = other.getEnd();
		} else if (o instanceof Annotation) {
			Annotation other = (Annotation) o;
			otherBegin = other.getBegin();
			otherEnd = other.getEnd();
		} else {
			return 0;
		}
		
		if (getStart() != otherBegin) {
			return getStart() - otherBegin;
		} else {
			return getEnd() - otherEnd;
		}
	}
	
	public boolean overlaps(Mention mention) {
		return mention != null
				&& ((getStart() >= mention.getStart() && getStart() < mention.getEnd())
				|| (getEnd() >= mention.getStart() && getEnd() < mention.getEnd())
				|| (mention.getStart() >= getStart() && mention.getStart() < getEnd())
				|| (mention.getEnd() >= getStart() && mention.getEnd() < getEnd()));
	}

	public String getDocumentText() {
		return documentText;
	}

	public void setDocumentText(String documentText) {
		this.documentText = documentText;
	}

	public ChemicalType getType() {
		return type;
	}

	public void setType(ChemicalType type) {
		this.type = type == null ? ChemicalType.UNKNOWN : type;
	}
}
