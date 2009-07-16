package com.fatwire.developernet.uri.lightweight;

import com.fatwire.cs.core.uri.Assembler;
import com.fatwire.cs.core.uri.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Lightweight abstract assembler that handles property management,
 * provides a logger, and handles encoding and decoding.  Much lighter
 * in weight than <code>com.fatwire.cs.core.uri.AbstractAssembler</code>.
 *
 * @author Tony Field
 * @since Sep 27, 2008
 */
public abstract class LightweightAbstractAssembler implements Assembler
{
    /**
     * Logger for use by sub-classes.
     */
    protected static final Log LOG = LogFactory.getLog(LightweightAbstractAssembler.class.getName());

    private static final String CHARSET_lower = "_charset_";
    private static final String CHARSET_upper = "_CHARSET_";

    private final String encoding;

    private final Map<String, String> properties = new HashMap<String, String>();

    /**
     * Constructor.  Upon object construction, support
     * for UTF-8 encoding is tested, and the result is cached
     * for future use in the encode() and decode() methods.
     * <p/>
     * UTF-8 is the recommended URLEncoding:
     * <ul>
     * <li><a href="http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars</a></li>
     * <li><a href="http://java.sun.com/j2ee/1.4/docs/tutorial/doc/WebI18N5.html">http://java.sun.com/j2ee/1.4/docs/tutorial/doc/WebI18N5.html</a></li>
     * <li><a href="http://www.ietf.org/rfc/rfc2396.txt">http://www.ietf.org/rfc/rfc2396.txt</a></li>
     * </ul>
     */
    protected LightweightAbstractAssembler()
    {
        String enc = "UTF-8";
        try
        {
            Util.encode("fake string", enc);
        }
        catch(UnsupportedEncodingException e)
        {
            LOG.warn("UTF-8 encoding not supported by this platform. Using the platform's default encoding as the URL encoding.");
            enc = null;
        }
        this.encoding = enc;
    }


    public void setProperties(Properties props)
    {
        Enumeration en = props.propertyNames();
        while(en.hasMoreElements())
        {
            String pName = (String)en.nextElement();
            String pValue = props.getProperty(pName);
            this.properties.put(pName, pValue);
        }
    }

    protected String getProperty(String name, String dephault)
    {
        String result = properties.get(name);
        if(result == null)
        {
            result = dephault;
        }
        return result;
    }

    /**
     * URLEncodes a string using the encoding specified by this class.
     *
     * @param string the string to encode
     * @return encoded string
     * @throws IllegalStateException if UTF-8 encoding is not supported
     * and the platform's default encoding is not supported.
     */
    protected final String encode(String string)
    {
        String result;
        try
        {
            if(string == null)
            {
                result = null;
            }
            else
            {
                result = Util.encode(string, encoding);
            }
        }
        catch(UnsupportedEncodingException ex)
        {
            String msg = "Unexpected failure encoding string '" + string + "'using an encoding (" + encoding + ").  Exception: " + ex;
            throw new IllegalStateException(msg);
        }
        return result;
    }

    /**
     * URLDecodes a string using the encoding specified by this class.
     *
     * @param string encoded string
     * @return decoded string
     * @throws IllegalStateException if UTF-8 encoding is not supported
     * and the platform's default encoding is not supported.
     * @throws IllegalArgumentException if the string is not well-formed for decoding.
     */
    protected final String decode(String string)
    {
        return decode(string, null);
    }

    /**
     * URLDecodes a string using the encoding specified.
     *
     * @param string encoded string
     * @param encoding the encoding to use to decode the string.  If null is specified, the decoding
     * specified by this class shall be used.
     * @return decoded string
     * @throws IllegalStateException if the encoding specified is not supported, or
     * if UTF-8 encoding is not supported
     * and the platform's default encoding is not supported.
     * @throws IllegalArgumentException if the string is not well-formed for decoding.
     */
    protected final String decode(String string, String encoding)
    {
        String result;
        if(string == null)
        {
            result = null;
        }
        else
        {
            if(encoding == null)
            {
                encoding = this.encoding;
            }
            try
            {
                result = Util.decode(string, encoding);
            }
            catch(IllegalArgumentException iae)
            {
                throw new IllegalArgumentException("Failure decoding string '" + string + "' using encoding '" + encoding + "'.  (" + iae.getMessage() + ")");
            }
            catch(UnsupportedEncodingException ex)
            {
                // This is not expected to ever occur.
                throw new IllegalStateException("Unexpected failure decoding string '" + string + "'using encoding '" + encoding + "'.  (" + ex + ")");
            }
        }
        return result;
    }

    /**
     * The multi-arg <code>java.net.URI</code> constructors quote illegal characters.  However,
     * this class requires that the query string already be properly URLEncoded.
     * As a result, we can't use the multi-arg URI constructor because all of
     * our % symbols and the + symbol will end up getting double-encoded.  So,
     * we need to construct a full URL ourselves so we can use the single-arg
     * URI constructor, because it does not quote anything.
     * <p/>
     * There are multiple variants of combinations of these parameters to create a valid URL.
     * Consult the URI specificaiton for what is allowed and what is not.  The URI constructor
     * will throw a URISyntaxException if required components are missing for a given combination.
     *
     * @param scheme the URI scheme (protocol)
     * @param authority the URI authority (host:port)
     * @param path the path for the URI (servlet context path, servlet name, pathinfo)
     * @param quotedQueryString the query string, with illegal characters already quoted.
     * @param fragment the fragment (anchor)
     * @return the valid URI with proper encoding
     * @throws URISyntaxException if there is a problem with what is passed in
     */
    protected static final URI constructURI(final String scheme, final String authority, final String path, final String quotedQueryString, final String fragment) throws URISyntaxException
    {
        // Update, Feb 25, 2005 by Tony Field
        StringBuilder bf = new StringBuilder();
        if(scheme != null)
        {
            bf.append(scheme).append(':'); // nothing legal can be quoted
        }
        if(authority != null)
        {
            bf.append("//").append(authority); // nothing legal to quote until I18N URLs work
        }
        // Path needs quoting though, so let the URI object do it for us.
        // Use the toASCIIString() method because we need the quoted values.
        // (toString() is really just for readability and debugging, not programmatic use)
        if(path != null)
        {
            bf.append(new URI(null, null, path, null, null).getRawPath());
        }
        if(quotedQueryString != null)
        {
            bf.append('?').append(quotedQueryString); // already quoted
        }
        // needs quoting
        if(fragment != null)
        {
            bf.append(new URI(null, null, null, null, fragment).toASCIIString());
        }
        URI uri = new URI(bf.toString());

        if(LOG.isDebugEnabled())
        {
            LOG.trace("Constructing new URI using the following components: \n" +
                      "scheme=" + scheme + " \n" +
                      "authority=" + authority + " \n" +
                      "path=" + path + " \n" +
                      "query=" + quotedQueryString + " \n" +
                      "fragment=" + fragment);

            LOG.debug("Assembled URI: " + uri.toASCIIString());
        }
        return uri;
    }

    /**
     * Parse a query string and put the parameters into a map.  Input
     * parameters will be URLDecoded prior to their addition into the
     * resultant map.
     * <p/>
     * Note that the map returned contains a <em><code>String[]</code> as the
     * value, not a single <code>String</code> value</em>
     * This provides support for query
     * strings with multiple values for a given parameter name.
     *
     * This decoding method is smart enough to be able to interpret the <code>_charset_</code>
     * URL parameter that is often used by IE.
     *
     * @param qry
     * @return map containing <code>String</code>/<code>String[]</code> pairs.
     * @throws IllegalArgumentException if there are mistakes
     * in the string that make it impossible to parse.
     */
    final protected Map<String, String[]> parseQueryString(String qry)
    {
        Map<String, String[]> rawPairs = new HashMap<String, String[]>();
        if(qry == null)
        {
            return rawPairs;
        }
        int inlen = qry.length();
        if(inlen == 0)
        {
            return rawPairs;
        }

        if(LOG.isTraceEnabled())
        {
            LOG.trace("Parsing query string: " + qry);
        }

        int iequal;
        int iamper;
        int startAt = 0;
        boolean bDone = false;

        while(!bDone)
        {
            String n;
            String v;
            if((iequal = qry.indexOf("=", startAt)) != -1)
            {
                // End of current name=value is '&' or EOL
                iamper = qry.indexOf("&", iequal);
                n = qry.substring(startAt, iequal);
                n = n.trim(); // deal with accidental odd chars in the URL
                iequal++;
                if(iequal >= inlen)
                {
                    break;
                }

                if(iamper == -1)
                {
                    v = qry.substring(iequal);
                }
                else
                {
                    v = qry.substring(iequal, iamper);
                }

                if(iamper != -1)
                {
                    startAt = iamper + 1;
                }
                else
                {
                    bDone = true;
                }

                v = v.trim(); // deal with stupid value

                // add the value to the result.
                String[] av = rawPairs.get(n);
                if(av == null)
                {
                    av = new String[1];
                    av[0] = v;
                    rawPairs.put(n, av);
                }
                else
                {
                    // param specified twice in the url.
                    String[] newVal = new String[av.length + 1];
                    System.arraycopy(av, 0, newVal, 0, av.length);
                    newVal[av.length] = v;
                    rawPairs.put(n, newVal);
                }
            }
            else
            {
                break;        // no more pairs
            }
        }

        // Figure out which encoding to use to decode the params
        String[] _charset_ = rawPairs.get(CHARSET_lower) == null ? rawPairs.get(CHARSET_upper) : rawPairs.get(CHARSET_lower);
        final String encoding;
        if(_charset_ == null)
        {
            encoding = null; // try to follow the spec
        }
        else
        {
            switch(_charset_.length)
            {
                case 0:
                    throw new IllegalStateException("Somehow an empty _charst_ param made it into our map. Impossible...");
                case 1:
                    encoding = _charset_[0]; // url contains an override for the spec
                    break;
                default:
                    throw new IllegalStateException("Too many values of _charset_ found in the URL");
            }
        }

        // Decode the raw pairs using the proper encoding and set them into the result map
        Map<String, String[]> res = new HashMap<String, String[]>(rawPairs.size());
        for(String rawKey : rawPairs.keySet())
        {
            String key = decode(rawKey, encoding);
            String[] val = rawPairs.get(rawKey);
            for(int i = 0; i < val.length; i++)
            {
                String rawVal = val[i];
                val[i] = decode(rawVal, encoding);

                if(LOG.isTraceEnabled())
                {
                    StringBuilder bf = new StringBuilder("Parsing query string.  Found raw pair [name]=[value]: ");
                    bf.append('[').append(rawKey).append(']').append('=').append('[').append(rawVal).append(']');
                    bf.append(" decoded to: ");
                    bf.append('[').append(key).append(']').append('=').append('[').append(val[i]).append(']');
                    LOG.trace(bf);
                }
            }
            res.put(key, val);
        }

        return res;
    }
}
