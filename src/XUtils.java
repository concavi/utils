/*
 * Copyright 2009-2012 Paolo Conte
 * This library is part of the Jelly framework.
 *
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
 */
package com.jellylab.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author paoloc
 */
public class XUtils
{

    public static class Strings
    {

        public static String nvl(String str, String replacement)
        {
            if (str != null && !str.equals("null"))
            {
                return str;
            }
            return replacement;
        }

        public static String nvl(String str)
        {
            return nvl(str, "");
        }

        public static String truncate(String str, int len)
        {
            if (str == null || str.length() <= len)
            {
                return str;
            }
            else
            {
                return str.substring(0, len);
            }
        }

        public static boolean isAlpha(String value)
        {
            boolean isAlpha = false;
            try
            {
                Long.parseLong(value);
            }
            catch (NumberFormatException nfexc)
            {
                // la stringa non è numerica
                isAlpha = true;
            }
            return isAlpha;
        }

        public static String escapeString(String str)
        {
            if (str == null)
            {
                return "";
            }
            String ret = "";
            int sz;
            sz = str.length();
            for (int i = 0; i < sz; i++)
            {
                char ch = str.charAt(i);

                // handle unicode
                if (ch > 0xfff)
                {
                    ret += "\\u" + hex(ch);
                }
                else if (ch > 0xff)
                {
                    ret += "\\u0" + hex(ch);
                }
                else if (ch > 0x7f)
                {
                    ret += "\\u00" + hex(ch);
                }
                else if (ch < 32)
                {
                    switch (ch)
                    {
                        case '\b':
                            ret += '\\';
                            ret += 'b';
                            break;
                        case '\n':
                            ret += '\\';
                            ret += 'n';
                            break;
                        case '\t':
                            ret += '\\';
                            ret += 't';
                            break;
                        case '\f':
                            ret += '\\';
                            ret += 'f';
                            break;
                        case '\r':
                            ret += '\\';
                            ret += 'r';
                            break;
                        default:
                            if (ch > 0xf)
                            {
                                ret += "\\u00" + hex(ch);
                            }
                            else
                            {
                                ret += "\\u000" + hex(ch);
                            }
                            break;
                    }
                }
                else
                {
                    switch (ch)
                    {
                        case '\'':
                            ret += '\\';
                            ret += '\'';
                            break;
                        case '"':
                            ret += '\\';
                            ret += '"';
                            break;
                        case '\\':
                            ret += '\\';
                            ret += '\\';
                            break;
                        case '/':
                            ret += '\\';		// CHECK
                            ret += '/';
                            break;
                        default:
                            ret += ch;
                            break;
                    }
                }

            }
            return ret;
        }

        public static String hex(char ch)
        {
            return Integer.toHexString(ch).toUpperCase();
        }

        public static String unescapeString(String escaped)
        {

            StringBuffer sb = new StringBuffer(escaped.length());

            for (int i = 0; i < escaped.length(); i++)
            {
                char ch = escaped.charAt(i);
                if (ch == '\\')
                {
                    char nextChar = (i == escaped.length() - 1) ? '\\' : escaped.charAt(i + 1);
                    // escape ottale
                    if (nextChar >= '0' && nextChar <= '7')
                    {
                        String code = "" + nextChar;
                        i++;
                        if ((i < escaped.length() - 1) && escaped.charAt(i + 1) >= '0'
                                && escaped.charAt(i + 1) <= '7')
                        {
                            code += escaped.charAt(i + 1);
                            i++;
                            if ((i < escaped.length() - 1) && escaped.charAt(i + 1) >= '0'
                                    && escaped.charAt(i + 1) <= '7')
                            {
                                code += escaped.charAt(i + 1);
                                i++;
                            }
                        }
                        sb.append((char) Integer.parseInt(code, 8));
                        continue;
                    }
                    switch (nextChar)
                    {
                        case '\\':
                            ch = '\\';
                            break;
                        case 'b':
                            ch = '\b';
                            break;
                        case 'f':
                            ch = '\f';
                            break;
                        case 'n':
                            ch = '\n';
                            break;
                        case 'r':
                            ch = '\r';
                            break;
                        case 't':
                            ch = '\t';
                            break;
                        case '\"':
                            ch = '\"';
                            break;
                        case '\'':
                            ch = '\'';
                            break;
                        // unicode esadecimale, formato: uXXXX
                        case 'u':
                            if (i >= escaped.length() - 5)
                            {
                                ch = 'u';
                                break;
                            }
                            int code = Integer.parseInt(
                                    "" + escaped.charAt(i + 2) + escaped.charAt(i + 3)
                                    + escaped.charAt(i + 4) + escaped.charAt(i + 5), 16);
                            sb.append((char) code);
                            i += 5;
                            continue;
                    }
                    i++;
                }
                sb.append(ch);
            }
            return sb.toString();
        }

        public static Result inStreamToString(InputStream inStream)
        {
            Writer writer = new StringWriter();
            BufferedInputStream bis = new BufferedInputStream(inStream);

            Result res = new Result();
            int bite;
            boolean readComplete = false;
            while (!readComplete)
            {
                try
                {
                    if (bis.available() > 0)
                    {
                        if ((bite = bis.read()) != -1)
                        {
                            writer.write(bite);
                        }
                    }
                    else
                    {
                        readComplete = true;
                    }
                }
                catch (IOException ioexc)
                {
                    readComplete = true;
                    res = new Result(ioexc);
                }
            }

            if (res.isOK())
            {
                // se OK ricreato con stringa generata
                return res.setObject(writer.toString());
            }
            else
            {
                return res;
            }
        }
    }

    public static class Numbers
    {

        public static String format(double val)
        {
            return format(val, 2);
        }

        public static String format(double val, int decs)
        {
            return format(val, decs, false);
        }

        public static String format(double val, int decs, boolean grouping)
        {
            NumberFormat nf = NumberFormat.getInstance(Locale.ITALY);
            nf.setGroupingUsed(grouping);
            nf.setMinimumFractionDigits(decs);
            nf.setMaximumFractionDigits(decs);
            return nf.format(val);
        }

        public static int giv(String str)
        {
            int value = 0;
            if (str == null)
            {
                return value;
            }
            str = str.trim();
            try
            {
                value = Integer.parseInt(str);
            }
            catch (Exception exc)
            {
            }
            return value;
        }

        public static int[] giv(String[] strs)
        {
            if (strs == null)
            {
                return new int[]
                        {
                        };
            }

            int[] values = new int[strs.length];
            for (int idv = 0; idv < strs.length; idv++)
            {
                values[idv] = giv(strs[idv]);
            }

            return values;
        }

        public static double gdv(String str)
        {
            double value = 0;
            boolean found = false;
            if (str == null || str.equals(""))
            {
                return 0;
            }

            try
            {
                // prova la conversione standard
                value = Double.parseDouble(str);
                found = true;
            }
            catch (Exception exc)
            {
            }

            if (!found)
            {
                NumberFormat nf = NumberFormat.getInstance(Locale.ITALIAN);
                try
                {
                    // prova la conversione da formato IT (m.cdu,dc)
                    value = nf.parse(str).doubleValue();
                    found = true;
                }
                catch (ParseException pex)
                {
                }
            }

            return value;
        }

        public static long glv(String str)
        {
            long value = 0;
            boolean found = false;
            if (str == null || str.equals(""))
            {
                return 0;
            }

            try
            {
                // prova la conversione standard
                value = Long.parseLong(str);
                found = true;
            }
            catch (Exception exc)
            {
            }

            if (!found)
            {
                NumberFormat nf = NumberFormat.getInstance(Locale.ITALIAN);
                try
                {
                    // prova la conversione da formato IT (m.cdu,dc)
                    value = nf.parse(str).longValue();
                    found = true;
                }
                catch (ParseException pex)
                {
                }
            }

            return value;
        }
    }

    public static class Files
    {

        public static final String EOF = "\r\n";

        public static Result copyDir(File origin, File dest) throws IOException
        {
            Result res;
            if (!origin.isDirectory())
            {
                res = new Result(new IllegalArgumentException("Origin is not a directory: " + origin.getPath()));
                return res;
            }

            if (!origin.exists())
            {
                res = new Result(new IllegalArgumentException("Origin doesn't exists: " + origin.getPath()));
                return res;
            }

            if (dest.exists())
            {
                res = new Result(new IllegalArgumentException("Destination directory already exists: " + dest.getPath()));
                return res;
            }

            dest.mkdirs();

            File[] files = origin.listFiles();
            for (int idf = 0; idf < files.length; idf++)
            {
                File file = files[idf];
                if (file.isDirectory())
                {
                    Result resCD = copyDir(file, new File(dest, file.getName()));
                    if (!resCD.isOK())
                    {
                        return resCD;
                    }
                }
                else
                {
                    Result resCP = copyFile(file, new File(dest, file.getName()));
                    if (!resCP.isOK())
                    {
                        return resCP;
                    }
                }
            }

            return new Result();
        }

        /**
         * Effettua la copia di un file
         * @param source File da copiare
         * @param dest File destinazione
         */
        public static Result copyFile(File source, File dest)
        {
            Result res;
            FileChannel fcSource = null;
            FileChannel fcDest = null;
            try
            {
                fcSource = new FileInputStream(source).getChannel();
                fcDest = new FileOutputStream(dest).getChannel();

                fcSource.transferTo(0, fcSource.size(), fcDest);
            }
            catch (IOException ioe)
            {
                res = new Result(ioe);
            }
            finally
            {
                try
                {
                    if (fcSource != null)
                    {
                        fcSource.close();
                    }
                    if (fcDest != null)
                    {
                        fcDest.close();
                    }
                }
                catch (IOException ioe)
                {
                }
            }
            return new Result();
        }

        public static Result fileToString(File source)
        {
            Result res = new Result();
            if (source == null)
            {
                res.addError("File is null");
                return res;
            }

            StringBuilder text = new StringBuilder();
            if (source.exists())
            {
                try
                {
                    String line = "";
                    BufferedReader inFile = new BufferedReader(new FileReader(source));
                    while ((line = inFile.readLine()) != null)
                    {
                        text.append(line).append(EOF);
                    }
                    inFile.close();
                }
                catch (IOException ioexc)
                {
                    res.addMessage(ioexc);
                    return res;
                }

                res.setObject(text.toString());
            }
            else
            {
                res.addError("File not found: " + source.getName());
            }

            return res;
        }

        public static class Randoms
        {

            private static final String CHAR_SEQUENCE_62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

            public static String timeID()
            {
                final int base = CHAR_SEQUENCE_62.length();
                long time = (new Date()).getTime();
                //time = Math.abs(1500000000000L - time);

                // WGID
                String ans = "";
                // resto della divisione
                int res = 0;
                // ricalcolo base
                while (time != 0)
                {
                    res = (int) (time % base);
                    time /= base;
                    ans = CHAR_SEQUENCE_62.charAt(res) + ans;
                }

                return ans;
            }

            public static double decodeTimeID(String tid)
            {
                final int base = CHAR_SEQUENCE_62.length();

                long time = 0;
                for (int idc = 0; idc < tid.length(); idc++)
                {
                    char ch = tid.charAt(idc);
                    time = time * base + CHAR_SEQUENCE_62.indexOf(ch);
                }

                return time;
            }
        }
    }

    public static class Urls
    {

        public static Result getText(String url)
        {
            Result res = new Result();
            if (url == null)
            {
                res.addError("URL is null");
                return res;
            }

            StringBuilder text = new StringBuilder();
            try
            {
                URL aUrl = new URL(url);
                BufferedReader br = new BufferedReader(new InputStreamReader(aUrl.openStream()));
                String line;
                while ((line = br.readLine()) != null)
                {
                    text.append(line).append(Files.EOF);
                }
                br.close();
            }
            catch (Exception exc)
            {
                res.addMessage(exc);
                return res;
            }

            res.setObject(text.toString());

            return res;
        }
    }

    public static class Dates
    {

        /** yyyyMMddHHmmss */
        public static final String FORMATO_DB = "yyyyMMddHHmmss";
        /** yyyyMMdd */
        public static final String FORMATO_DB_NO_ORE = "yyyyMMdd";
        /** dd/MM/yyyy */
        public static final String FORMATO_IT = "dd/MM/yyyy";
        /** HH:mm:ss */
        public static final String FORMATO_ORE = "HH:mm:ss";
        /** HH:mm:ss HH:mm:ss.SSS*/
        public static final String FORMATO_DATA_COMPLETA = "dd/MM/yyyy HH:mm:ss.SSS";
        /** EEE, dd MMM yyyy HH:mm:ss zzz */
        public static final String FORMATO_DATA_RSS2 = "EEE, dd MMM yyyy HH:mm:ss zzz";

        public static String formatDate(Date mydate, String pattern)
        {
            SimpleDateFormat fmt = new SimpleDateFormat(pattern);
            return fmt.format(mydate);
        }

        public static DateTime nStringDB2DateTime(String sdata)
        {
            DateTimeFormatter fmt = DateTimeFormat.forPattern(FORMATO_DB);
            DateTime dateTime = fmt.parseDateTime(sdata);

            return dateTime;
        }

        public static DateTime nStringIT2DateTime(String sdata)
        {
            DateTimeFormatter fmt = DateTimeFormat.forPattern(FORMATO_IT);
            DateTime dateTime = fmt.parseDateTime(sdata);

            return dateTime;
        }

        public static Date nStringIT2Date(String sdata)
        {
            SimpleDateFormat sdfmt = new SimpleDateFormat(FORMATO_IT);
            Date data = null;
            try
            {
                data = sdfmt.parse(sdata);
            }
            catch (ParseException pexc)
            {
            }
            catch (NullPointerException npexc)
            {
            }

            return data;
        }

        public static Date nStringDB2Date(String sdata)
        {
            SimpleDateFormat sdfmt = new SimpleDateFormat(FORMATO_DB);
            Date data = null;
            try
            {
                data = sdfmt.parse(sdata);
            }
            catch (ParseException pexc)
            {
            }
            catch (NullPointerException npexc)
            {
            }

            return data;
        }

        public static String nDate2StringIT(Date data)
        {
            return nDate2StringIT(data, false);
        }

        public static String nDate2StringIT(Date data, boolean ore)
        {
            if (data == null)
            {
                return "";
            }
            String pattern = FORMATO_IT;
            if (ore)
            {
                pattern += " " + FORMATO_ORE;
            }
            SimpleDateFormat sdfmt = new SimpleDateFormat(pattern);
            String sdata = null;
            sdata = sdfmt.format(data);

            return sdata;
        }

        public static String nDate2StringDB(Date data)
        {
            return nDate2StringDB(data, false);
        }

        public static String nDate2StringDB(Date data, boolean forzaOreZero)
        {
            if (data == null)
            {
                return "";
            }

            String formato = FORMATO_DB;
            if (forzaOreZero)
            {
                formato = FORMATO_DB_NO_ORE;
            }
            SimpleDateFormat sdfmt = new SimpleDateFormat(formato);
            String sdata = null;
            sdata = sdfmt.format(data);
            if (forzaOreZero)
            {
                sdata += "000000";
            }

            return sdata;
        }

        public static String nConverti(String dataDB)
        {
            return nConverti(dataDB, false);
        }

        public static String nConverti(String dataDB, boolean ore)
        {
            Date data = nStringDB2Date(dataDB);

            return nDate2StringIT(data, ore);
        }

        public static String nPrepara(String dataIT)
        {
            Date data = nStringIT2Date(dataIT);

            return nDate2StringDB(data);
        }

        public static String nStringDateFormat(String strData, String patternIn, String patternOut)
        {
            if (strData == null)
            {
                return "";
            }

            SimpleDateFormat sdfmt = new SimpleDateFormat(patternIn);
            Date data = null;
            try
            {
                data = sdfmt.parse(strData);
            }
            catch (ParseException pexc)
            {
            }
            catch (NullPointerException npexc)
            {
            }

            if (data != null)
            {
                sdfmt = new SimpleDateFormat(patternOut);
                strData = "";
                strData = sdfmt.format(data);
            }
            else
            {
                strData = "";
            }

            return strData;
        }

        public static String nDateFormat(Date data, String pattern)
        {
            String strData = "";
            if (data != null)
            {
                SimpleDateFormat sdfmt = new SimpleDateFormat(pattern);
                // la data RSS richiede il formato con locale "EN"
                if (pattern.equals(FORMATO_DATA_RSS2))
                {
                    sdfmt = new SimpleDateFormat(pattern, Locale.ENGLISH);
                }
                strData = "";
                strData = sdfmt.format(data);
            }
            else
            {
                strData = "";
            }

            return strData;
        }

        public static Date addDate(Date data, int gg, int mm, int yy)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(data);
            if (gg != 0)
            {
                cal.add(Calendar.DATE, gg);
            }
            if (mm != 0)
            {
                cal.add(Calendar.MONTH, mm);
            }
            if (yy != 0)
            {
                cal.add(Calendar.YEAR, yy);
            }

            return cal.getTime();
        }

        public static boolean isPasqua(Date date)
        {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);

            int year = calendar.get(Calendar.YEAR);
            int dateYMD = year * 10000
                    + calendar.get(Calendar.MONTH) * 100
                    + calendar.get(Calendar.DAY_OF_MONTH);
            Date pasqua = find(year);
            calendar.setTime(pasqua);
            int pasquaYMD = year * 10000
                    + calendar.get(Calendar.MONTH) * 100
                    + calendar.get(Calendar.DAY_OF_MONTH);

            return (pasquaYMD == dateYMD);
        }

        private static Date find(int year)
        {
            int a = year % 19;
            int b = year % 4;
            int c = year % 7;

            int m = 0;
            int n = 0;

            if ((year >= 1583) && (year <= 1699))
            {
                m = 22;
                n = 2;
            }
            if ((year >= 1700) && (year <= 1799))
            {
                m = 23;
                n = 3;
            }
            if ((year >= 1800) && (year <= 1899))
            {
                m = 23;
                n = 4;
            }
            if ((year >= 1900) && (year <= 2099))
            {
                m = 24;
                n = 5;
            }
            if ((year >= 2100) && (year <= 2199))
            {
                m = 24;
                n = 6;
            }
            if ((year >= 2200) && (year <= 2299))
            {
                m = 25;
                n = 0;
            }
            if ((year >= 2300) && (year <= 2399))
            {
                m = 26;
                n = 1;
            }
            if ((year >= 2400) && (year <= 2499))
            {
                m = 25;
                n = 1;
            }

            int d = (19 * a + m) % 30;
            int e = (2 * b + 4 * c + 6 * d + n) % 7;

            Calendar calendar = new GregorianCalendar();
            calendar.set(Calendar.YEAR, year);

            if (d + e < 10)
            {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, Calendar.MARCH);
                calendar.set(Calendar.DAY_OF_MONTH, d + e + 22);
            }
            else
            {
                calendar.set(Calendar.MONTH, Calendar.APRIL);
                int day = d + e - 9;
                if (26 == day)
                {
                    day = 19;
                }
                if ((25 == day) && (28 == d) && (e == 6) && (a > 10))
                {
                    day = 18;
                }
                calendar.set(Calendar.DAY_OF_MONTH, day);
            }

            return calendar.getTime();
        }
    }

    public static class Result
    {

        public static final int UD = 0;
        public static final int OK = 1;
        public static final int KO = 2;
        public static final int WARNING = 4;
        public static final int INFO = 8;
        public List messages;
        private XObject xob = null;

        public Result()
        {
            messages = new ArrayList();
        }

        public Result(Object obj)
        {
            messages = new ArrayList();
            xob = new XObject(obj);
        }

        public Result addMessage(int type, String text)
        {
            return addMessage(type, text, text);
        }

        public Result addMessage(Exception exc)
        {
            xob = new XObject(exc);
            return addMessage(KO, exc.getMessage(), "");
        }

        public Result addMessage(int type, String text, String content)
        {
            Message messaggio = new Message();
            messaggio.type = type;
            messaggio.text = text;
            messaggio.content = content;
            messages.add(messaggio);
            return this;
        }

        public Result addMessage(Message messaggio)
        {
            messages.add(messaggio);
            return this;
        }

        public Result addInfo(String text)
        {
            return addMessage(INFO, text, text);
        }

        public Result addError(String text)
        {
            return addMessage(KO, text, text);
        }

        public Result addError(String text, String content)
        {
            return addMessage(KO, text, content);
        }

        /**
         * Aggiunge n messaggii, generati autonomamente o provenienti da un altro Risultato
         */
        public Result addMessaggi(List altriMessaggi)
        {
            messages.addAll(altriMessaggi);
            return this;
        }

        /**
         * Restituisce il primo messaggio, da utilizzare se un metodo può generare un solo errore
         * Verificare la presenza di messaggi o del risultato restituito a null
         */
        public Message getFirstMessage()
        {
            if (hasMessages())
            {
                return (Message) messages.get(0);
            }
            return null;
        }

        /**
         * Restituisce la lista dei messaggi presenti in questo Risultato
         */
        public List getMessages()
        {
            return messages;
        }

        /**
         * Verifica se sono presenti messaggi
         */
        public boolean hasMessages()
        {
            return messages.size() > 0;
        }

        public Result setObject(Object obj)
        {
            return setXObject(new XObject(obj));
        }

        public Result setXObject(XObject xob)
        {
            this.xob = xob;
            return this;
        }

        public XObject getXObject()
        {
            return xob;
        }

        /**
         * Verifica che i messaggi, se presenti, siano tutti di tipo OK
         */
        public boolean isOK()
        {
            // se non ci sono messaggi è OK
            if (messages.isEmpty())
            {
                return true;
            }

            // se ci sono messaggi verifica che siano tutti OK
            boolean foundKO = false;
            for (int idm = 0; idm < messages.size(); idm++)
            {
                Message messaggio = (Message) messages.get(idm);
                if (messaggio.type != OK && messaggio.type != INFO)
                {
                    foundKO = true;
                    break;
                }
            }

            return !foundKO;
        }

        public String toString()
        {
            String val = xob.toString() + "[";
            for (int idm = 0; idm < messages.size(); idm++)
            {
                Message messaggio = (Message) messages.get(idm);
                val += messaggio.toString();
                if (idm < messages.size() - 1)
                {
                    val += ", ";
                }
            }
            val += "]";
            return val;
        }

        public String toHTML()
        {
            String html = "result:";
            if (xob != null)
            {
                html += " object=" + xob;
            }
            html += "<br/>";
            for (int idm = 0; idm < messages.size(); idm++)
            {
                Message messaggio = (Message) messages.get(idm);
                html += messaggio.toHTML();
            }
            return html;
        }

        private class Message
        {

            public int type = 0;
            public String text = "";
            public String content = "";

            public String toString()
            {
                String val = "";
                switch (type)
                {
                    case OK:
                        val += "OK";
                        break;
                    case KO:
                        val += "KO";
                        break;
                    case WARNING:
                        val += "WARNING";
                        break;
                }
                val += ": ";
                val += text;
                val += " | ";
                val += content;
                return val;
            }

            public String toHTML()
            {
                String html = "<p><strong>";
                switch (type)
                {
                    case OK:
                        html += "OK";
                        break;
                    case KO:
                        html += "KO";
                        break;
                    case WARNING:
                        html += "WARNING";
                        break;
                }
                html += "</strong>: ";
                html += text;
                return html;
            }
        }
    }

    public static void localTest()
    {
        Date oggi = new Date();
        String dbDate = XUtils.Dates.nDate2StringDB(oggi);
        System.out.println(dbDate);
        String itDate = XUtils.Dates.nConverti(dbDate);
        System.out.println(itDate);
        System.out.println(XUtils.Dates.nPrepara(itDate));
        System.out.println(XUtils.Dates.nDate2StringDB(oggi, true));

        XObject xob = new XObject(new Error("Test"));
        System.out.println(xob);
    }

    public static void main(String[] args)
    {
        localTest();
    }
}
