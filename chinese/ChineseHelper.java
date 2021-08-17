/* ////////////////////////////////////////////////////////////////////////
 * ChineseHelper.java - Static data and methods for Chinese characters
 *   and strings.
 *
 *   Copyright (C) 2010-2013    Yun-Tung Lau
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

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.Map.Entry;
import java.net.URL;
import java.util.logging.Logger;

import util.ArrayHelper;
import util.StringHelper;
import util.StringHelper.Comparison;
import io.StreamHelper;

/** 
 * Static data and methods for Chinese characters and strings.
 * <p>
 *  See {@link CCharacter CCharacter.java} for the object design.
 * <p>
 *  See {@link util.StringHelper StringHelper.java} for methods to tighten
 *  strings with Big5 code.
 * <p>
 *  Note: Big5 code is a 2-byte code, first two Hex digits ranging
 *  from A1 to FE, last two digits from 40 to 7E or A1 to FE.
 *
 * <h3>History</h3>
 * <ul>
 * <li>2010-02 Added unicode characters as the default for digits.
 * <li>2013-08 Initial version with character form conversion and comparison.
 * <li>2013-09 Moved enumeration of options for comparison to StringHelper.java.
 *   Added compareNatural(), toInt(), isDigit(), isPower10Chinese(), etc.
 *   Changed digits to char[].
 * </ul> 
 */
public class ChineseHelper {

  /** Flag for debugging. */
  public static int debug = 1;

  /** Logger for logging. */
  public static Logger logger = null;

  /** Default CSV file name. 
   *  See {@link #load(String csvFile) load(csvFile)} 
   *  for search rule of this file.
   */
  public static final String CSV_FILE = "chinese.csv";

  /** Pattern for matching the current traditional char. */
  public static Pattern currentCharPattern = Pattern.compile("[~\uff5e]");

  /** Pattern for splitting concepts. */
  public static Pattern conceptDilimiter = Pattern.compile("[,\uff0c]");

  /** Pattern for splitting vocabluaries in a concept. */
  public static Pattern vocabDilimiter = Pattern.compile("[;\uff1b\u3002]");

  /** Vector of CCharacter. */
  private static Vector<CCharacter> cCharV = new Vector<CCharacter>();

  /** Map from character to CCharacter[]. */
  private static TreeMap<Character, CCharacter[]> charToCChar 
    = new TreeMap<Character, CCharacter[]>();

  /** Map from vocabluary string to CCharacter[]. */
  private static TreeMap<String, CCharacter[]> vocabToCChar
    = new TreeMap<String, CCharacter[]>();

  /** Digits in Unicode. digits[1] is the character for 1, etc.
   *  Note digits[10] is ten in Chinese.
   */
  public static final char[] digits = {
    '\u96f6','\u4e00','\u4e8c','\u4e09','\u56db','\u4e94',
    '\u516d','\u4e03','\u516b','\u4e5d','\u5341'
  };

  /** Digits in Big5. */
  public static final String[] digitsBig5 = {
    "零","一","二","三","四","五",
    "六","七","八","九","十",
  };

  /** 
   * Load the Chinese character data from
   * the input stream and create the object structure and mappings for use by
   * the static methods.  The data should be in UTF-8 format.
   * The input stream is left open.
   * 
   * @param in InputStream for the Chinese character data
   * @throws Exception If an error occurred when reading from the 
   *   input stream, or the input stream contains a malformed 
   *   ...
   */
  public static final void load(InputStream in) throws Exception {
    if (in == null) throw new Exception("ChineseHelper.load: null input stream");

    String line;
    while ((line = StreamHelper.readLine(in, "UTF-8")) != null) {
      if (line.length() == 0 || line.charAt(0) == '#') continue; // skip empty or comment line 
      String[] parts = conceptDilimiter.split(line);
      
      String chars = parts[0];
      
      int clen = chars.length();
      if (clen < 2) {
        show("ChineseHelper.load: invalid line '" + line + "'");
        continue;
      }
      
      CCharacter cchar = new CCharacter(chars.charAt(1), chars.charAt(0));
      
      // Add map entries for each characters
      for (int i = 0; i < clen; i++) {
        if (i > 0 && chars.charAt(i) == chars.charAt(i-1)) continue;
        Character c = new Character(chars.charAt(i));
        CCharacter[] cca = charToCChar.get(c);
	if (cca != null) {
	  int m = cca.length;
	  cca = Arrays.copyOf(cca, m+1); 
	  cca[m] = cchar;
          charToCChar.put(c, cca);  // add mapping to new array
        } else {
	  cca = new CCharacter[1];
	  cca[0] = cchar;
          charToCChar.put(c, cca);
        }
      }
      
      if (clen > 2) { // Has variant characters
        cchar.variants = new char[clen-2];
	for (int i = 0; i < clen-2; i++) {
	  cchar.variants[i] = chars.charAt(i+2);
	}
      }
      
      int len = parts.length;
      Pronunciation pn = null;
      if (len >= 2) { // Has pronunciation
        pn = new Pronunciation(parts[1]);
      }

      cchar.pronounce = pn;
      
      if (len >= 3) { // Has concepts
	for (int i = 0; i < len-2; i++) {
	  parts[i+2] = currentCharPattern.matcher(parts[i+2]).replaceAll(chars.substring(1,2));  // replace ~ with trad. char.  (There could be existing simp. char)
	  String[] vocabs = vocabDilimiter.split(parts[i+2]);
	  cchar.addConcept(new Concept(vocabs));
	  
	  // add vocabs to mapping
	  for (String v : vocabs) {
            CCharacter[] cca = vocabToCChar.get(v);
	    if (cca != null) {
	      int m = cca.length;
	      cca = Arrays.copyOf(cca, m+1); 
	      cca[m] = cchar;
              vocabToCChar.put(v, cca);  // add mapping to new array
            } else {
	      cca = new CCharacter[1];
	      cca[0] = cchar;
              vocabToCChar.put(v, cca);
            }	    
          }
	}
      }
      
      cCharV.add(cchar);
    }

    // convert vocabs to simp and trad forms and add them to mapping
    String[] vocabs = vocabToCChar.keySet().toArray(new String[vocabToCChar.size()]);
    for (String v : vocabs) {
      CCharacter[] cca = vocabToCChar.get(v);
      
      String vSimp = toSimplified(v);    
      CCharacter[] cca1 = vocabToCChar.get(vSimp);
      if (cca1 != cca) { // implying cca1 != null
        cca = ArrayHelper.combineUnique(cca, cca1);
      }
      
      String vTrad = toTraditional(v);
      cca1 = vocabToCChar.get(vTrad);
      if (cca1 != cca) {  // implying cca1 != null
        cca = ArrayHelper.combineUnique(cca, cca1);
      }
      
      vocabToCChar.put(vSimp, cca);
      vocabToCChar.put(vTrad, cca);
    }

    if (debug >= 2) {
      show("Created " + cCharV.size() + " CCharacters.");
    }
  }

  /** 
   * Load the Chinese character data from the input file.  
   * It first check input file on 
   * the current directory.  If not found, it looks into the system resource
   * (JAR file under "util/"), where the input file should be under the chinese 
   * directory (same as ChineseHelper.class).
   * 
   * @param csvFile Name of CSV file containing Chinese character data.
   *   If empty, use the default CSV_FILE.
   * @throws Exception From {@link #load(InputStream in)}
   */
  public static final void load(String csvFile) throws Exception {
    if (isEmpty(csvFile)) csvFile = CSV_FILE;

    File f = new File(csvFile);  
    URL url;

    // try file first, then from system resources
    if (f != null && f.exists()) {  // read from disk file

      FileInputStream fis = new FileInputStream(f);
      try {
        load(fis);
        show("ChineseHelper.load: got " + cCharV.size() + " Chinese phonetic characters from " + csvFile);
      } finally {
        fis.close();
      }

    } else if ((url = ClassLoader.getSystemResource("chinese/" + csvFile)) != null) { 
      // found as system resource

      InputStream in = ClassLoader.getSystemResourceAsStream("chinese/" + csvFile);
      try {
        load(in);
        show("ChineseHelper.load: got " + cCharV.size() + " Chinese phonetic characters from " + url);
      } finally {
        in.close();
      }

    } else {
      show("ChineseHelper.load: cannot find " + csvFile);
    }

  }

  /** 
   * Save the Chinese character data (cCharV) as CSVs to the specified file.  
   * The data are in the same order as they are read. 
   *
   * @param csvFile Name of CSV file for saving the data.
   * @throws Exception If the input file name is empty.
   */
  public static final void save(String csvFile) throws Exception {
    if (isEmpty(csvFile)) throw new Exception("ChineseHelper.save: empty input file name");

    File f = new File(csvFile);
    FileOutputStream fos = new FileOutputStream(f);
    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
    try {
      for (CCharacter cChar : cCharV) {
        String tmp = cChar.toString();
        osw.write(tmp, 0, tmp.length());
	osw.write(StreamHelper.CR);
	osw.write(StreamHelper.LF);
      }
      
    } finally {
      osw.close();
      fos.close();
    }
  }

  /** 
   * Check whether the simpl, trad, and variant forms appear in cannonical
   * order.  Output those not in order to the specified file.  
   * Note: About 1600 characters are not in order!
   *
   * @param csvFile Name of CSV file for saving the data.
   * @throws Exception If the input file name is empty.
  public static final void checkCanonicalOrder(String csvFile) throws Exception {
    if (isEmpty(csvFile)) throw new Exception("ChineseHelper.checkCanonicalOrder: empty input file name");

    File f = new File(csvFile);
    FileOutputStream fos = new FileOutputStream(f);
    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
    try {
      for (CCharacter cChar : cCharV) {
        boolean variantInOrder = true;
	if (cChar.variants != null && cChar.variants.length > 0) {
	  for (char vc : cChar.variants) {
	    if (vc < cChar.simpChar || vc < cChar.tradChar) {
	      variantInOrder = false;
	      break;
	    }
	  }
	}
	
        if (cChar.simpChar > cChar.tradChar || !variantInOrder) {
          String tmp = cChar.toString();
          osw.write(tmp, 0, tmp.length());
	  osw.write(StreamHelper.CR);
	  osw.write(StreamHelper.LF);
	}
      }
      
    } finally {
      if (osw != null) osw.close();
      if (fos != null) fos.close();
    }
  }
  */

  /** 
   * Convert the input file to the specified form and write the data 
   * to the output file.  Options for conversion are simplified 
   * and traditional Chinese forms.
   *
   * @param inFile Name of input file
   * @param outFile Name of output file
   * @param traditional If true, convert to traditional form, otherwise convert to
   *  simplified form.
   * @throws Exception If the input or output file name is empty.
   */
  public static final void convert(String inFile, String outFile, boolean traditional)
  throws Exception {
    if (isEmpty(inFile)) throw new Exception("ChineseHelper.save: empty input file name");
    if (isEmpty(outFile)) throw new Exception("ChineseHelper.save: empty output file name");

    File fi = new File(inFile);
    FileInputStream fis = new FileInputStream(fi);
    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");

    File fo = new File(outFile);
    FileOutputStream fos = new FileOutputStream(fo);
    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");

    try {
      char[] line;
      while ((line = StreamHelper.readLine(isr)) != null) {
        String tmp = (traditional) ? toTraditional(line) : toSimplified(line);
        osw.write(tmp, 0, tmp.length());
        osw.write(StreamHelper.CR);
        osw.write(StreamHelper.LF);
      }
      
    } finally {
      isr.close();
      fis.close();

      osw.close();
      fos.close();
    }

  }

  /** For any Chinese characters in the input character array, find the
   *  corresponding CCharacter and return them as an array.
   * <p>
   *  All other form conversion methods in this class eventually call
   *  this method.  So it loads the CSV file if not already.
   *
   * @param chars A character array
   * @return An array of CCharacter.  The array element is null for any
   *  input character without mapping to a CCharacter.
   * @throw Exception If the input argument is null.
   */
  public static CCharacter[] toCChars(char[] chars) throws Exception {
    if (charToCChar.size() == 0) load(ChineseHelper.CSV_FILE);
    if (chars == null) throw new Exception("ChineseHelper.toCChars: input array is null");
    int n = chars.length;
    CCharacter[] cchars = new CCharacter[n];
    
    for (int i = 0; i < n; i++) {
      Character c = new Character(chars[i]);
      CCharacter[] cca = charToCChar.get(c);
      if (cca != null && cca.length > 0) {
        CCharacter cc = null;
	if (cca.length == 1) {
	  cchars[i] = cca[0];
	  if (debug >= 4) show(">> " + c + " => " + cca[0].toString());

	} else { // try using vocabs of lengths [2,4] to narrow it down.
	  String[] vocabs = new String[9];
	  if (i > 0) vocabs[0] = String.valueOf(chars, i-1, 2);
	  if (i <= n-2) vocabs[1] = String.valueOf(chars, i, 2);
	  if (i > 1) vocabs[2] = String.valueOf(chars, i-2, 3);
	  if (i > 0 && i <= n-2) vocabs[3] = String.valueOf(chars, i-1, 3);
	  if (i <= n-3) vocabs[4] = String.valueOf(chars, i, 3);
	  if (i > 2) vocabs[5] = String.valueOf(chars, i-3, 4);
	  if (i > 1 && i <= n-2) vocabs[6] = String.valueOf(chars, i-2, 4);
	  if (i > 0 && i <= n-3) vocabs[7] = String.valueOf(chars, i-1, 4);
	  if (i <= n-4) vocabs[8] = String.valueOf(chars, i, 4);
	  
	  CCharacter[] cca2 = null;
	  // Search in reverse so longer vocab strings have priority
          for (int j = vocabs.length-1; j >= 0; j--) {
	    String s = vocabs[j];
	    if (s == null) continue;
	    cca2 = vocabToCChar.get(s);	    
	    if (cca2 != null) {
	      if (debug >= 2) show(">> " + c + "/" + s + " => " + cca2[0].toString());
	      break;
	    }
	  }
	  
	  if (cca2 == null || cca2.length == 0) { // no mapping from vocab
  	    cchars[i] = cca[0];  // pick first one	
  
	  } else {  // even if cca2 has only 1 element, need to be sure it overlaps cca
	    for (CCharacter cc2 : cca2) {  // find overlap of two sets
 	      for (CCharacter cc1 : cca) {
	        if (cc1 == cc2) {
		  cchars[i] = cc1;
		  break;
		}
	      }
	      if (cchars[i] != null) break;
            }
  	    
	    if (cchars[i] == null) cchars[i] = cca[0]; // pick first one if no overlap
	  }	    
	}

      } // if no mapping found, just skip it
    }
    
    return cchars;
  }
 
  /** Determines whether the two input strings are the same when the
   *  Chinese character form (traditional, simplified, or variant) is ignored.
   *
   * @param s1 First input string
   * @param s2 Second input string
   * @return True if the two input strings are the same when the
   *  Chinese character form is ignored.
   */
  public static boolean equalsIgnoreForm(String s1, String s2)
  throws Exception {
    return equalsIgnoreForm(s1.toCharArray(), s2.toCharArray());
  }
  
  /** Determines whether the two input char arrays are the same when the
   *  Chinese character form (traditional, simplified, or variant) is ignored.
   *
   * @param chars1 First character array
   * @param chars2 Second character array
   * @return True if the two input char arrays strings are the same when the
   *  Chinese character form is ignored.
   */
  public static boolean equalsIgnoreForm(char[] chars1, char[] chars2)
  throws Exception {
    if (chars1.length != chars1.length) return false;

    CCharacter[] cca1 = ChineseHelper.toCChars(chars1);
    CCharacter[] cca2 = ChineseHelper.toCChars(chars2);
    int n1 = cca1.length, n2 = cca2.length;
    
    if (n1 != n2) return false;
    for (int i=0; i < n1; i++) {
      if (cca1[i] != cca2[i] || 
        (cca1[i] == null && cca2[i] == null) && chars1[i] != chars2[i]) {
        return false;
      }
    }
    
    return true;
  }

  /** Compare the two input char arrays.  Comparison of 
   *  Chinese character form (simplified, traditional, or variant) is treated
   *  according to the input option.
   *  <p>
   *  The comparison logic is as follows:
   *  For each character pairs from the two array, if
   * <ol>
   * <li>Both map to chinese character, compare their simplified form lexicographically
   * </li>
   * <li>Only one map to a chinese character, compare the input character with 
   *  the simplified form of the mapped chinese character lexicographically
   * </li>
   * <li>Both have no mapping to chinese character, compare the input characters
   *  according to the input option.
   * </li>
   * </ol>
   * <p>
   * Caution: Calling program must handle the case of null input parameter, since this 
   * method does not check them or throw exception.
   *   
   * @param chars1 First character array
   * @param chars2 Second character array
   * @param option One of the option for comparison.
   *   See {@link util.StringHelper.Comparison StringHelper.Comparison}.
   *   LEXICAL is just the standard Java system string comparison.
   *   FOLD_CASE is treated the same as FOLD_FORM, and 
   *   IGNORE_CASE is treated the same as IGNORE_FORM.
   * @return Zero if the char arrays are the same based on the comparison option.
   *   A positive integer if s1 > s2, negative if s1 &lt; s2.
   */
  public static int compare(char[] chars1, char[] chars2, Comparison option) {
    if (option == Comparison.LEXICAL) {
      return new String(chars1).compareTo(new String(chars2));
    }
    
    int result = 0;
    
    CCharacter[] cca1, cca2;    
    try {
      cca1 = ChineseHelper.toCChars(chars1);
      cca2 = ChineseHelper.toCChars(chars2);
    } catch (Exception e) {
      // If for some reason ChineseHelper could not map the characters,
      // the arrays will be compared lexicographically
      if (option == Comparison.IGNORE_FORM || option == Comparison.FOLD_FORM
       || option == Comparison.IGNORE_CASE || option == Comparison.FOLD_CASE
       || option == Comparison.PINYIN_FOLD_FORM) { 
        return new String(chars1).compareToIgnoreCase(new String(chars2));
      } else {
        return new String(chars1).compareTo(new String(chars2));
      }
    }
      
    int n1 = cca1.length, n2 = cca2.length;

    int i1=0, i2=0;
    while (true) {
      char c1 = (i1 >= n1) ? 0 : chars1[i1];
      char c2 = (i2 >= n2) ? 0 : chars2[i2];
      
      if (c1 == 0 && c2 == 0) { // both reach the end
        if (option == Comparison.IGNORE_FORM || option == Comparison.IGNORE_CASE) { 
          return 0;
        } else if (option == Comparison.FOLD_FORM || option == Comparison.FOLD_CASE
	  || option == Comparison.PINYIN_FOLD_FORM) { 
          return new String(chars1).compareTo(new String(chars2));
        } else {
          return 0;
        }
      }

      if (c1 == c2) {  // a slight optimization by skipping forward if the same
        i1++; i2++;
        continue;
      }

      CCharacter cc1 = (i1 >= n1) ? null : cca1[i1];
      CCharacter cc2 = (i2 >= n2) ? null : cca2[i2];
   
      if (cc1 == null && cc2 == null) { // no mapping to CCharacter
        if (option == Comparison.IGNORE_FORM || option == Comparison.FOLD_FORM
         || option == Comparison.IGNORE_CASE || option == Comparison.FOLD_CASE
	 || option == Comparison.PINYIN_FOLD_FORM) { 
          // map forward & backward
          c1 = Character.toUpperCase(Character.toLowerCase(c1));
          c2 = Character.toUpperCase(Character.toLowerCase(c2));
	}

        result = c1 - c2; // compare them lexicographically	
        if (result != 0) return result; // return upon difference
	
      } else if (cc1 == null) {
        return c1 - cc2.simpChar;
	
      } else if (cc2 == null) {
        return cc1.simpChar - c2;
	
      } else { // both have mapping to CCharacter
        result = cc1.simpChar - cc2.simpChar; // compare simplified char
        if (result != 0) return result; // return upon difference
      }
      
      i1++; i2++;
    }    
  }

  /** Compare the two input strings using the input option.  This is a
   *  convenient method for invoking {@link chinese.ChineseHelper#compare(char[], char[], util.StringHelper.Comparison) compare(chars1, chars2, option)}.
   *
   * @param s1 First input string
   * @param s2 Second input string
   * @param option One of the option for comparison.
   *   See {@link util.StringHelper.Comparison StringHelper.Comparison}.
   * @return Zero if the strings are the same based on the comparison option.
   *   A positive integer if s1 > s2, negative if s1 &lt; s2.
   */
  public static int compare(String s1, String s2, Comparison option) {
    return compare(s1.toCharArray(), s2.toCharArray(), option);
  }

  /** Compare the two input strings.  The
   *  Chinese character form (traditional, simplified, or variant) is folded.
   *  <p>
   *  Note that compareFoldForm is nearly the same as compareIgnoreForm.
   *  However, after form folding, it will perform an additional lexico
   *  comparison.  Typically the simplified will appear in front of traditional.
   *
   * @param s1 First input string
   * @param s2 Second input string
   * @return Zero if the two input strings are the same when
   *   Chinese character form is folded.
   *   A positive integer if s1 > s2, negative if s1 &lt; s2.
   */
  public static int compareFoldForm(String s1, String s2) {
    return compare(s1.toCharArray(), s2.toCharArray(), Comparison.FOLD_FORM);
  }

  /** Compare the two input strings.  The
   *  Chinese character form (traditional, simplified, or variant) is ignored.
   *
   * @param s1 First input string
   * @param s2 Second input string
   * @return Zero if the two input strings are the same when
   *   Chinese character form is ignored.
   *   A positive integer if s1 > s2, negative if s1 &lt; s2.
   */
  public static int compareIgnoreForm(String s1, String s2) {
    return compare(s1.toCharArray(), s2.toCharArray(), Comparison.IGNORE_FORM);
  }

  /**
   * Compare the two input character arrays in a 'natural' fashion.  That is,
   * collapse the spaces, treat number pair with priority and compare 
   * them as integers.
   * Otherwise, if there is no number pair to compare, or the numbers 
   * are the same, compare them according to the input option.
   * <p>
   * This method is an extension of {@link util.StringHelper#compareNatural(char[] ca1, char[] ca2, Comparison option) StringHelper.compareNatural()}.
   * <p>
   * The algorithm is as follows:
   * <p>
   * (1) Check whether there is any Chinese character in the input arrays.  If not
   * simply invoke {@link util.StringHelper#compareNatural(char[] ca1, char[] ca2, Comparison option) StringHelper.compareNatural()}.
   * <p>
   * (2) Convert Chinese characters representing power of 10 (such as &#21313;,
   * &#30334;) to integer digits, such that the sequence of chinese numbers 
   * forms a decimal number similar to arabic numbers.  For the characters
   * &#21313; and &#30334;, the conversion rules are (X, Y, Z are Chinese numbers):
   * <pre><code> &#21313;Y &rarr; 1Y
   * X&#21313; &rarr; X&#38646;
   * X&#21313;Y &rarr; XY
   * X&#30334; &rarr; X&#38646;&#38646;
   * X&#30334;Y&#21313; &rarr; XY&#38646;
   * X&#30334;Y&#21313;Z &rarr; XYZ</code></pre>
   * Note that a sequence containing only Chinese characters representing power of 10
   * is not changed.
   * <p>
   * (3) Arabic or Chinese numbers (&#38646; to  &#21313;) are treated the same
   * and are ordered before English alphabets.
   * <p>
   * (4) Chinese of different forms, or English alphabets of lower or upper cases
   * can be folded or ignored, based on the input comparison option.
   * Note that the option FOLD_FORM is nearly the same as IGNORE_FORM.  It only
   * perform a lexical comparison if the two string are considered the same.
   * As a result, the following is in ascending order,
   * "&#20061;&#26847;&#20116;&#27155;" &lt; "&#20061;&#26635;&#21313;&#19968;&#27155;", even though
   * "&#26847" &gt; "&#26635".
   * <p>
   * The testing code is in {@link test.TestChineseHelper test.TestChineseHelper}.
   * <p>
   * Caution: The calling program must handle the case of null input parameter, 
   * since this method does not check them or throw exception.
   *
   * @param chars1 First character array
   * @param chars2 Second character array
   * @param option One of the option for comparison.
   *   See {@link util.StringHelper.Comparison StringHelper.Comparison}.
   *   LEXICAL is default.
   *   FOLD_CASE is treated the same as FOLD_FORM, and 
   *   IGNORE_CASE is treated the same as IGNORE_FORM.
   *   PINYIN_FOLD_FORM is not yet implemented.
   * @return Zero if the char arrays are the same based on the comparison option.
   *   A positive integer if s1 &gt; s2, negative if s1 &lt; s2.
   * @see test.TestChineseHelper 
   */
  public static final int compareNatural(char[] ca1, char[] ca2, Comparison option) {
  
    if (!containsUnicodeCJK(ca1) && !containsUnicodeCJK(ca2)) {
      Comparison op;
      switch (option) { // map the option to those for StringHelper.compareNatural
        case IGNORE_FORM: 
	  op = Comparison.IGNORE_CASE;
	  break;
        case FOLD_FORM:
	case PINYIN_FOLD_FORM:
          op = Comparison.FOLD_CASE;
	  break;
	case LEXICAL:
	default:
	  op =  Comparison.LEXICAL;
	  break;
      }
      return StringHelper.compareNatural(ca1, ca2, op);
    }
    
    char[] chars1 = toDecimal(ca1);
    char[] chars2 = toDecimal(ca2);
    
    CCharacter[] cca1, cca2;    
    try {
      cca1 = ChineseHelper.toCChars(chars1);
      cca2 = ChineseHelper.toCChars(chars2);
    } catch (Exception e) {
      // If for some reason ChineseHelper could not map the characters,
      // the arrays will be compared lexicographically
      if (option == Comparison.IGNORE_FORM || option == Comparison.FOLD_FORM
       || option == Comparison.IGNORE_CASE || option == Comparison.FOLD_CASE
       || option == Comparison.PINYIN_FOLD_FORM) { 
        return new String(chars1).compareToIgnoreCase(new String(chars2));
      } else {
        return new String(chars1).compareTo(new String(chars2));
      }
    }

    int n1 = cca1.length, n2 = cca2.length;

    int i1=0, i2=0;
    boolean skipped = false;
    while (true) {
      char c1 = (i1 >= n1) ? 0 : chars1[i1];
      char c2 = (i2 >= n2) ? 0 : chars2[i2];

      // skip leading spaces
      while (Character.isSpaceChar(c1)) {
        skipped = true;
        i1++;
        c1 = (i1 >= n1) ? 0 : chars1[i1];
      }

      while (Character.isSpaceChar(c2)) {
        skipped = true;
        i2++;
        c2 = (i2 >= n2) ? 0 : chars2[i2];
      }

      // if either reach the end (c1 or c2 == 0), it will be handled below
      boolean c1isDigit = ChineseHelper.isDigit(c1);
      boolean c2isDigit = ChineseHelper.isDigit(c2);

      // if both are digits, compare consecutive digits by advancing
      // them simultaneously until either one is not a digit or ends.
      // The number with more digits wins.  If they have the same number
      // of digits, the number with the higher leading digit wins.
      int result = 0;
      if (c1isDigit && c2isDigit) {

        // skip leading zeros but stop before a non-digit
        while (c1 == '0') {
          int ii = i1 + 1;
          char cc = (ii >= n1) ? 0 : chars1[ii];
          if (!ChineseHelper.isDigit(cc)) break;
          i1 = ii;
          c1 = cc;
          skipped = true;
        }
        while (c2 == '0') {
          int ii = i2 + 1;
          char cc = (ii >= n2) ? 0 : chars2[ii];
          if (!ChineseHelper.isDigit(cc)) break;
          i2 = ii;
          c2 = cc;
          skipped = true;
        }

        while (true) {
	  //  c1 and c2 should contain leading digits
          if (result == 0) result = ChineseHelper.toInt(c1) - ChineseHelper.toInt(c2);

          i1++;
          c1 = (i1 >= n1) ? 0 : chars1[i1];
          i2++;
          c2 = (i2 >= n2) ? 0 : chars2[i2];

          c1isDigit = ChineseHelper.isDigit(c1);
          c2isDigit = ChineseHelper.isDigit(c2);
          if (!c1isDigit && !c2isDigit) { 
	    // both reach the end or become non-digit.  Use the last result
            break;
          } else if (!c1isDigit) { // chars1 ends or becomes non-digit first
 	    return -1;
          } else if (!c2isDigit) { // chars2 ends or becomes non-digit first
 	    return 1;
          } 

	}

        // at this point, non-zero result indicates different numbers
        if (result != 0) return result;
      }
	
      // At this point, either the numbers are the same and both c1 and c2 
      // are not digit, or the strings are the same
      // lexicographically after collapsing spaces, or either c1 and c2 are not digit.

      // Both reach the end.  To ensure a consistent result for sorting:
      // * For IGNORE_FORM, compare IGNORE_FORM if there was 
      //   spaces or zeros skipped. If nothing was skipped, return 0.
      // * For FOLD_FORM, compare lexicographically
      //   in order to maintain a predefined (lexical) order.
      // * For the default of LEXICAL, do lexicographical comparison 
      //   if there was spaces or zeros skipped. If nothing was skipped, return 0.
      // 
      if (c1 == 0 && c2 == 0) {
        if (option == Comparison.IGNORE_FORM || option == Comparison.IGNORE_CASE) { 
          return (skipped) ? compare(chars1, chars2, Comparison.IGNORE_FORM) : 0;
        } else if (option == Comparison.FOLD_FORM || option == Comparison.FOLD_CASE
	  || option == Comparison.PINYIN_FOLD_FORM) { 
	  return new String(chars1).compareTo(new String(chars2)); 
        } else { 
          return (skipped) ? new String(chars1).compareTo(new String(chars2)) : 0;
        }
      }

      // At this point, both are the same when compared as numbers up to their
      // current positions, or there was no digit.

      if (c1 == c2) {  // a slight optimization by skipping forward if the same
        i1++; i2++;
        continue;
      }

      CCharacter cc1 = (i1 >= n1) ? null : cca1[i1];
      CCharacter cc2 = (i2 >= n2) ? null : cca2[i2];
   
      if (option == Comparison.LEXICAL) {
        result = (c1isDigit ? ChineseHelper.toInt(c1) : c1)
          - (c2isDigit ? ChineseHelper.toInt(c2) : c2); 
	  // compare them lexicographically after mapping to integer as needed
        if (result != 0) return result; // return upon difference
      
      } else if (cc1 == null && cc2 == null) { // no mapping to CCharacter
        if (option == Comparison.IGNORE_FORM || option == Comparison.IGNORE_CASE
         || option == Comparison.FOLD_FORM || option == Comparison.FOLD_CASE
	 || option == Comparison.PINYIN_FOLD_FORM) { 
          // map forward & backward
          c1 = Character.toUpperCase(Character.toLowerCase(c1));
          c2 = Character.toUpperCase(Character.toLowerCase(c2));
	}

        result = (c1isDigit ? ChineseHelper.toInt(c1) : c1)
          - (c2isDigit ? ChineseHelper.toInt(c2) : c2); 
	  // compare them lexicographically after mapping to integer as needed
        if (result != 0) return result; // return upon difference
	
      } else if (cc1 == null) {
        return (c1isDigit ? ChineseHelper.toInt(c1) : c1) 
	  - (c2isDigit ? ChineseHelper.toInt(c2) : cc2.simpChar);
	
      } else if (cc2 == null) {
        return (c1isDigit ? ChineseHelper.toInt(c1) : cc1.simpChar) 
	  - (c2isDigit ? ChineseHelper.toInt(c2) : c2);
	
      } else { // both have mapping to CCharacter.  Compare integer or simplified char
        if (option == Comparison.IGNORE_FORM || option == Comparison.IGNORE_CASE
         || option == Comparison.FOLD_FORM || option == Comparison.FOLD_CASE) {
         result = (c1isDigit ? ChineseHelper.toInt(c1) : cc1.simpChar)
           - (c2isDigit ? ChineseHelper.toInt(c2) : cc2.simpChar);
	} else if (option == Comparison.PINYIN_FOLD_FORM) { 
	  // No yet implemented.  Treated as FOLD_FORM for now
         result = (c1isDigit ? ChineseHelper.toInt(c1) : cc1.simpChar)
           - (c2isDigit ? ChineseHelper.toInt(c2) : cc2.simpChar);
        }
        if (result != 0) return result; // return upon difference
      }
      
      i1++; i2++;
    }
  }
  
  /** Compare the two input strings naturally using the input option.  This is a
   *  convenient method for invoking {@link chinese.ChineseHelper#compareNatural(char[] chars1, char[] chars2, Comparison option) compareNatural(chars1, chars2, option)}.
   *
   * @param s1 First input string
   * @param s2 Second input string
   * @param option One of the option for comparison.
   *   See {@link util.StringHelper.Comparison StringHelper.Comparison}.
   * @return Zero if the strings are the same based on the comparison option.
   *   A positive integer if s1 &gt; s2, negative if s1 &lt; s2.
   */
  public static int compareNatural(String s1, String s2, Comparison option) {
    return compareNatural(s1.toCharArray(), s2.toCharArray(), option);
  }

  /**
   * Compare the two input Strings in a 'natural' fashion. 
   * The comparison is lexicographical.
   * See {@link util.StringHelper.Comparison#LEXICAL Comparison.LEXICAL}.
   *
   * @param s1 First input string
   * @param s2 Second input string
   * @return Zero if the two strings are the same based on the comparison.
   *   A positive integer if s1 &gt; s2, negative if s1 &lt; s2.
   */
  public static final int compareNatural(String s1, String s2) {
    return compareNatural(s1.toCharArray(), s2.toCharArray(), Comparison.LEXICAL);
  }

  /** For any Chinese characters in the input character array, convert them 
   *  to the traditional form.
   *
   * @param chars A character array
   * @return A string in the traditional form and is equal to the 
   *  input string when the Chinese character form is ignored.
   */
  public static String toTraditional(char[] chars) throws Exception {
    CCharacter[] cca = ChineseHelper.toCChars(chars);
  
    StringBuilder sb = new StringBuilder();    
    int n = chars.length;
    for (int i = 0; i < n; i++) {
      CCharacter cc = cca[i];
      if (cc != null) {
	sb.append(cc.tradChar);
      } else { // no mapping found, use original
        sb.append(chars[i]);
      }
    }
    return sb.toString();
  }

  /** For any Chinese characters in the input string, convert them 
   *  to the traditional form.
   *
   * @param s First input string
   * @return A string in the traditional form and is equal to the 
   *  input string when the Chinese character form is ignored.
   */
  public static String toTraditional(String s) throws Exception {
    return toTraditional(s.toCharArray());
  }
  
  /** For any Chinese characters in the input character array, convert them 
   *  to the simplified form.
   *
   * @param s First input string
   * @return A string in the simplified form and is equal to the 
   *  input string when the Chinese character form is ignored.
   */
  public static String toSimplified(char[] chars) throws Exception {
    CCharacter[] cca = ChineseHelper.toCChars(chars);
  
    StringBuilder sb = new StringBuilder();    
    int n = chars.length;
    for (int i = 0; i < n; i++) {
      CCharacter cc = cca[i];
      if (cc != null) {
	sb.append(cc.simpChar);
      } else { // no mapping found, use original
        sb.append(chars[i]);
      }
    }
    return sb.toString();
  }

  /** For any Chinese characters in the input string, convert them 
   *  to the simplified form.
   *
   * @param s First input string
   * @return A string in the simplified form and is equal to the 
   *  input string when the Chinese character form is ignored.
   */
  public static String toSimplified(String s) throws Exception {
    return toSimplified(s.toCharArray());
  }

  /** Determine whether the input character is a
   *  unicode CJK Unified Ideograph (Han) in the range 4e00-9fcf.
   * @param c A character
   * @return True if the input character is a
   *  unicode CJK Unified Ideograph, false otherwise.
   */
  public static boolean isUnicodeCJK(char c) {
    return (c >= '\u4e00' && c <= '\u9fcf');
  }
  
  /** Determine whether the input character array contains 
   *  unicode CJK Unified Ideographs (Han) in the range 4e00-9fcf.
   * @param chars A character array
   * @return True if the input character array contains
   *  unicode CJK Unified Ideographs, false otherwise.
   */
  public static boolean containsUnicodeCJK(char[] chars) {
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] >= '\u4e00' && chars[i] <= '\u9fcf') return true;
    }
    return false;
  }

  /** Determine whether the input string contains 
   *  unicode CJK Unified Ideographs (Han) in the range 4e00-9fcf.
   * @param s A string
   * @return True if the input string contains
   *  unicode CJK Unified Ideographs, false otherwise.
   */
  public static boolean containsUnicodeCJK(String s) {
    return containsUnicodeCJK(s.toCharArray());
  }
  
  /** The Chinese characters that represent powers of ten.
   */
  public static char[] power10Chineses = { '\u5341', '\u767e', '\u5343',
    '\u4e07','\u842c', '\u4ebf', '\u5104' };

  /** The power of ten represented by the Chinese characters.
   */
  public static int[] power10s = { 1, 2, 3, 4, 4, 8, 8 };

  /** Determine whether the input character is
   *  Chinese character that represents a power of ten.
   * @param c A character
   * @return True if the input character represents a power of ten, false otherwise.
   */
  public static boolean isPower10Chinese(char c) {
    for (int i = 0; i < power10Chineses.length; i++) {
      if (power10Chineses[i] == c) return true;
    }
    return false;
  }

  /** Determine whether the input character is
   *  Chinese character that represents a power of ten.  If so, return the power.
   *  Otherwise, return -1.
   * @param c A character
   * @return A power of ten, or -1 if the input Chinese character does not
   *  represent a power of ten.
   */
  public static int toPower10(char c) {
    for (int i = 0; i < power10Chineses.length; i++) {
      if (power10Chineses[i] == c) return power10s[i];
    }
    return -1;
  }
  
  /** Convert Chinese numbers in the input characters to decimal integer format.
   *  Specifically, it converts the Chinese characters that represent power of ten
   *  to digits.
   *   
   * @param chars A character array that may contain Chinese numbers
   * @return A new character array in which Chinese numbers are presented in decimal
   *  integer format.
   */
  public static char[] toDecimal(char[] chars) {
    StringBuilder sb = new StringBuilder();

    int i1 = 0, i2 = 0;
    int i = 0;
    
    while (i < chars.length) {
      char c = chars[i];    
  
      if (isPower10Chinese(c)) {
        boolean hasDigit = false;
        int j = i - 1;
        while (j >= i2) { // find contigeous digits going backward
	  if (isDigit(chars[j])) {
	    hasDigit = true;
	    j--;
	  } else {
	    break;
	  }
	}
	i1 = j+1; // inclusive range start

	// copy those in [i2, i1)
	j = i2;
        while (j < i1) {
	  sb.append(chars[j]);
	  j++;
        }
	
        j = i + 1;
        while (j < chars.length) { // find contigeous digits or Power10Chinese going forward
	  if (isDigit(chars[j])) {
	    hasDigit = true;
	    j++;
          } else if (isPower10Chinese(chars[j])) {
	    j++;
	  } else {
            break;
	  }
	}
	i2 = j; // exclusive range end
	
	// convert those in [i1, i2)
	if (!hasDigit) { // no conversion needed if there is no digits
 	  j = i1;
          while (j < i2) {
	    sb.append(chars[j]);	
	    j++;
          }

	} else {
 	  j = i1;
          while (j < i2) {
	    c = chars[j];
	    if (isPower10Chinese(c)) {
	      if (j == i1) { // first one => number of the power
  	        sb.append(toPower10(c));
	      } else if (j == i2-1) { // last one => n zeros
	        for (int k=0; k < toPower10(c); k++) sb.append('0');
	      } // else omit it
	    } else {
	      sb.append(c);
	    }
	    j++;
          }
	}
	
        i = i2; // advance to next range
	
      } else {  // isPower10Chineses was false
        i++;
      }
      
    }
    
    // finally copy those in [i2, chars.length)
    int j = i2;
    while (j < chars.length) {
      sb.append(chars[j]);
      j++;
    }

    return sb.toString().toCharArray();
  }
  
  /** Convert the input character to a positive integer, if it represents
   *  a digit (0-9, or the Chinese character for zero through ten).
   * @param c A character representing a digit
   * @return An integer 0-10, or -1 if the input character does not represent a digit.
   */
  public static int toInt(char c) {
    if (Character.isDigit(c)) {
      return c - 48;
    } else {
      for (int i = 0; i < digits.length; i++) {
        if (digits[i] == c) return i;
      }
      return -1;
    }
  }

  /** Determine whether the input character represents
   *  a digit (0-9, or the Chinese character for zero through nine).
   * @param c A character
   * @return True if the input character represents a digit, false otherwise.
   */
  public static boolean isDigit(char c) {
    if (Character.isDigit(c)) {
      return true;
    } else {
      for (int i = 0; i < digits.length; i++) {
        if (digits[i] == c) return true;
      }
      return false;
    }
  }
  
   /** Indicate whether the string is null or "".
   */
  public static final boolean isEmpty(String s) {
    return (s == null || s.length() == 0);
  }

  /** 
   * Show the input string.  If logger is not null, send the string there.
   * @param String to be shown.
   */
  public static final void show(String s) {
    if (logger != null) logger.info(s);
    else System.out.println(s);
  }

}
