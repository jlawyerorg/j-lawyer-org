/*******************************************************************************
 * Bizcal is a component library for calendar widgets written in java using swing.
 * Copyright (C) 2007  Frederik Bertilsson 
 * Contributors:       Martin Heinemann martin.heinemann(at)tudor.lu
 * 
 * http://sourceforge.net/projects/bizcal/
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 * 
 *******************************************************************************/
package bizcal.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Copies the content from one stream to another.
 *
 * @author Fredrik Bertilsson
 */
public class StreamCopier
{
    public static void copy(InputStream anInStream, OutputStream anOutStream)
            throws IOException
    {
        byte[] bs = new byte[8192];
        int length;
        while ((length = anInStream.read(bs)) != -1) {
            anOutStream.write(bs,0,length);
        }
        anOutStream.flush();
        anOutStream.close();
        anInStream.close();
    }

    public static String copy(InputStream anInStream)
            throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(anInStream));
        StringBuffer result = new StringBuffer();

        String line = reader.readLine();
        while (line != null) {
            result.append(line + "\n");
            line = reader.readLine();
        }
        reader.close();
        return result.toString();
    }

    public static byte[] copyToByteArray(InputStream anInStream)
            throws IOException
    {
        BufferedInputStream inputStream = new BufferedInputStream(anInStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream outputStream = new BufferedOutputStream(byteArrayOutputStream);

        int ch = inputStream.read();
        while (ch != -1) {
            outputStream.write(ch);
            ch = inputStream.read();
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

}
