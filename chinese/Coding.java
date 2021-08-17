/* ////////////////////////////////////////////////////////////////////////
 * Coding.java - Static methods for Chinese encodings.
 *
 *   Copyright (C) 2008-2010    Yun-Tung Lau
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

/** 
 * Static methods for Chinese encodings.
 */
public class Coding {

  /** Detect Big5 double bytes and return the number of bytes
   *  consistent with Big5 encoding.  It scans from "start"
   *  and stop at the first non-Big5 code. 
   * <P>
   * Note: <A HREF="http://ash.jp/code/cn/big5tbl.htm">Big5</A>
   * codes are 2-bytes, first pair of Hex digits ranging
   * from A1 to FE, last pair of Hex digits from 40 to 7E and A1 to FE.
   *
   * @param buf  The input byte array.
   * @param start  The starting index to check. 
   * @return The number of bytes consistent with Big5 encoding.
   *  This number is always even.  Return zero if buf is empty or 
   *  no Big5 codes are detected.
   */
  public static final int detectBig5(byte [] buf, int start) {
    if (buf == null || buf.length < 2) return 0;
    int i;
    for (i=start; i<buf.length-1; i += 2) {
      int b1 = buf[i] & 0xFF;
      if (b1 < 0xA1 || b1 > 0xFE) break;
      int b2 = buf[i+1] & 0xFF;
      if (b2 < 0x40 || (b2 > 0x7E && b2 < 0xA1) || b2 > 0xFE) break;
    }
    return (i-start);  // always even, or zero
  }

  /** Count the number of Big5 characters.  It scans from "start"
   *  to the end of the input byte array. 
   * <P>
   * Note: <A HREF="http://ash.jp/code/cn/big5tbl.htm">Big5</A>
   * codes are 2-bytes, first pair of Hex digits ranging
   * from A1 to FE, last pair of Hex digits from 40 to 7E and A1 to FE.
   *
   * @param buf  The input byte array.
   * @param start  The starting index to count. 
   * @return The number of Big5 characters represented by double bytes
   *  in the byte array.  Return zero if buf is empty or 
   *  no Big5 codes are detected.
   */
  public static final int countBig5(byte [] buf, int start) {
    if (buf == null || buf.length < 2) return 0;
    int count = 0;
    int i;
    for (i=start; i<buf.length-1; ) {
      int b1 = buf[i] & 0xFF;
      if (b1 < 0xA1 || b1 > 0xFE) { // first byte not matching, go to next
        i++;
        continue;
      } 

      int b2 = buf[i+1] & 0xFF;
      if (b2 < 0x40 || (b2 > 0x7E && b2 < 0xA1) || b2 > 0xFE) {
        // second byte not matching, go to next pair
        i += 2;
        continue;
      }

      count++;
      i += 2;
      continue;
    }
    return count;
  }

}
