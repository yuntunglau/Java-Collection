/* ////////////////////////////////////////////////////////////////////////
 * CCharacter.java - This class encapsulates a Chinese character in various forms.
 *
 *   Copyright (C) 1998-2013    Yun-Tung Lau
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *   
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ////////////////////////////////////////////////////////////////////////
 *
 */
//*************************************************************************
   
package chinese;

import java.util.*;

/** 
 * This class encapsulates a Chinese character in various forms.  It
 * relates the Chinese character to a pronunciation in the context 
 * of one or more concepts.
 * <p>
 * The object design is as follows:
 * <pre>
 *     CCharacter [*]--->[0..1] Pronunciation
 *         [1]
 *          |
 *         [*]
 *       Concept [1]----[*] vocabulary string
 * </pre>
 * It has the following properties:
 * <ol>
 * <li>For each CCharacter, there are zero or more Concepts.
 * <li>For each CCharacter, there is zero or one Pronunciation.
 * <li>For each Concept, there is one CCharacter.
 * </ol> 
 *
 * <p>
 * The class ChineseHelper contains a vector of all CCharacter.
 * It also has a map from a character to CCharacter[] and a map
 * from a vocabluary string to CCharacter[].
 * <p>
 * The structure and methods are symmetric with respect to simplified
 * and traditional Chinese characters.  All conversion or matching of
 * Chinese characters are handled by the method 
 * {@link ChineseHelper#toCChars(char[] chars) ChineseHelper.toCChars(char[])},
 * which relates the input character array into CCharacter[].
 * <p>
 * Once the CCharacter is identified for a character, one can navigate to 
 * the corresponding Pronunciation and Concepts.
 *
 * <h3>History</h3>
 * <ul>
 * <li>1998-06 Initial version.
 * <li>2013-07 Moved static data to ChineseHelper.java
 * </ul> 
 */
public class CCharacter {

  /** The character in traditional form. */
  public char tradChar;

  /** The character in simplified form. */
  public char simpChar;

  /** The characters in variant forms. */
  public char[] variants;

  /** The pronunciation for the CCharacter. */
  public Pronunciation pronounce;

  /** The concepts related to this CCharacter. */
  private Vector<Concept> concepts = new Vector<Concept>();
  
  /** Constructor */
  public CCharacter(char tradChar, char simpChar) {
    this.tradChar = tradChar;
    this.simpChar = simpChar;
  }

  /** Add the input concept to this CCharacter. */
  public void addConcept(Concept concept) {
    concepts.add(concept);
  }

  /** Returns a string representation of this object. */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(simpChar).append(tradChar);
    if (variants != null && variants.length > 0) sb.append(variants);
    if (pronounce != null) sb.append(',').append(pronounce.toString());
    if (concepts.size() > 0) {      
      for (Concept c : concepts) {
        sb.append(',').append(c.toString());
      }
    }
    return sb.toString();
  }
}
