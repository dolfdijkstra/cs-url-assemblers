/*
 * Copyright (c) 2009 FatWire Corporation. All Rights Reserved.
 * Title, ownership rights, and intellectual property rights in and
 * to this software remain with FatWire Corporation. This  software
 * is protected by international copyright laws and treaties, and
 * may be protected by other law.  Violation of copyright laws may
 * result in civil liability and criminal penalties.
 */
package com.fatwire.developernet.uri.itemcontext;

import com.fatwire.cs.core.uri.*;
import com.fatwire.cs.core.uri.Definition.ContainerType;
import com.fatwire.developernet.uri.lightweight.LightweightAbstractAssembler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * <p>In this assembler, URLs have the following form:
 * <code>{protocol}://{domain}/{context path}/{servlet name}/{item-context}/{item-type}/{item-alias}/v{variant number}</code>.
 * </p>
 * <p>Generally, this means that an individual item and its context can be represented in the URI.
 * If the item itself is not required, its type and identifier can both be suppressed from the URI.
 * In addition, a numerical variant can also be suppressed.  Thus, the assembler can be written
 * as follows:
 * <code>[{protocol}://{domain}]/{context path}/{servlet name}/{item-context}[/{item-type}/{item-alias}][/v{variant number}]</code>.
 * where [] denote optional components.</p>
 * <p>In most use cases, the {item-context} maps to the BurlingtonFinancial and FirstSiteII rendering
 * models' <code>p</code> variable.  The {item-type} maps to the asset type, or <code>c</code>, and
 * the {item-alias} maps to the asset id, or <code>cid</code>.  However, it may also be the case that the
 * {item-type} indicates some sort of arbitrary information like a product catalogue or an external
 * resource like a user profile system, etc. while the {item-alias} could represent a path in a catalogue,
 * a user id, etc.</p>
 * <p>Examples of supported urls:</p>
 * <ul>
 * <li>http://localhost/cs/Satellite/home</li>
 * <li>http://localhost/Satellite/company/contact-info (in this example, {context path} is null, that is, the servlet
 * context root is blank.)</li>
 * <li>http://localhost/cm/company/news/press-releases/2009/08/03/civic-holiday.html (In this example, {context path
 * is null, and the Satellite servlet is referenced using a url-pattern of "cm". This is configured in web.xml).</li>
 * <li>http://localhost/cm/company/media/press-kit/downloads/logo-full (In this example, the path to the asset
 * in the site plan tree is <code>company/media/press-kit</code>, and thus that denotes the {item-context} for the URL.
 * It is defined by the variable <code>p</code>.  The value of <code>p</code> would be the id of the <code>press-kit</code>
 * page placed under the <code>media</code> page..  The asset type alias is <code>downloads</code> which will probably
 * map to an asset type like <code>Document</code> or <code>PDF</code>, corresponding to the variable <code>c</code>.
 * Finally, the {item-alias} parameter maps to <code>logo-full</code> which maps to an individual asset, corresponding to
 * the <code>cid</code> variable.</li>
 * <li>http://localhost/cs/Satellite/brand-x/catalogue/electronics/audio-players/ipod/v2</li>
 * </ul>
 * <p>It is the intent that this assembler could be used to assemble any URL that utilizes
 * the site plan tree and an asset id, that is, <code>c</code>, <code>cid</code>, and <code>p</code>
 * to uniquely identify every page on the site.  It was also designed with support for multi-element
 * (i.e. multiple slashes) values for both {item-context} and {item-alias}.  However, only
 * a single path element is permitted for {item-type}.</p>
 * <p>This assembler must be used in conjunction with a helper class that is called from
 * within Content Server to calculate {item-context} and {item-alias} when links are being
 * created, as well as to resolve the original source variables (like <code>c</code>,
 * <code>cid</code>, and <code>p</code> for example) when the page is parsed.  If the
 * FirstSiteII rendering model or a related rendering model is being used, this typically
 * means that the computation of {item-context} and {item-alias} is done in the <code>Link</code>
 * templates, and the decoding is done in the <code>Wrapper</code> templates.</p>
 * <p>If decoding happens in a template that is uncached, it may be required for the decoder
 * to have built-in caching support.</p>
 * <p>Helper classes are independent of this assembler, though it is likely that a SitePlanHelper will ship
 * alongside this assembler.</p> TODO: document this.
 * <p/>
 * <p><strong>Requirements</strong></p>
 * <p>Note: This assembler does NOT support BlobServer URLs using the structure above.  BlobServer URLs are delegated
 * to a fallback assembler which can be configured, or defaults back to the QueryAssmbler.  This assembler is hard-wired
 * to require Satellite Server rendering pages.  Other types of URLs will be delegated to another assembler.</p>
 * <p/>
 * <p/>
 * todo: finish documentation
 * <p/>
 * make note that this will check c if item-type is not set, and will re-populate both c and item-type as the same value
 * <p/>
 * <p/>
 * <p/>
 * <p>These three variable elements - page path, asset type, and asset name, are the key
 * variables required to map to the standard rendering variables c, cid, and p.
 * On assembly, the assembler will look for "item-context", "c", and "cpath" variables.
 * If they are found, it will attempt to assemble the URL.  If they are not found,
 * no attempt will be made.</p>
 * <p>Variant is an optional parameter that exists to support multivariate testing.  The
 * value it is set to must be an integer, but the parameter is optional.</p>
 * <p/>
 * <p>This assembler supports aliases for the asset types, so instead of rendering "Page",
 * an alternate string could be used instead.  The same would apply to other asset types.
 * This mapping is configured via properties.</p>
 * <p/>
 * <p>This assembler is designed only to work with Satellite URLs for page content.  No
 * other URL types are supported by this assembler.</p>
 * <p/>
 * <p>On disassembly, at least 3 elements must be found in the path info.  Secondly, the
 * second-last element must be a registered asset type.  (How registration is done can
 * be left to the developer, however it must be property-based.)</p>
 * <p>In order that item-context and cname arrive in the URL as available parameters, they must
 * be set into the render:gettemplateurl tag as arguments in each asset's link template.
 * To ensure they are set consistently, a java function accompanies this class and should
 * be called in each Link template.</p>
 * <p>In order that item-context and cname are appropriately resolved, the decoding function needs
 * to be called in teh wrapper/controller.</p>
 * <p>Configuration</p>
 * <table>
 * <tr><th>property</th><th>value</th></tr>
 * <tr><td>uri.assembler.SitePlanPpathCCpathAssembler.assettype.lookup.[TYPE]</td><td>asset type alias for asset type [TYPE]</td></tr>
 * <tr><td>uri.assembler.SitePlanPpathCCpathAssembler.assettype.alias.[ALIAS]</td><td>asset type name for asset type aliase [ALIAS]</td></tr>
 * <tr><td>uri.assembler.SitePlanPpathCCpathAssembler.layout.[domain name [:port]]</td><td>name of template for layout to be used for the domain name specified.  The port should be specified if it is in the URL.</td></tr>
 * <tr><td>uri.assembler.SitePlanPpathCCpathAssembler.wrapper.[domain name [:port]]</td><td>name of site entry for wrapper template to be used for the domain specified.  This is the default site entry value.</td></tr>
 * </table>
 *
 * @author Tony Field
 * @since May 14, 2009
 */
public final class ItemContextAssembler extends LightweightAbstractAssembler
{
    private static final Log LOG = LogFactory.getLog("com.fatwire.developernet.uri.itemcontext");

    public static final String PROP_ITEM_TYPE_PARAMETER_PREFIX = "com.fatwire.developernet.uri.itemcontext.item-type.parameter.";
    public static final String PROP_ITEM_TYPE_ALIAS_PREFIX = "com.fatwire.developernet.uri.itemcontext.item-type.alias.";
    public static final String PROP_ITEM_TYPE_FOR_CONTEXT = "com.fatwire.developernet.uri.itemcontext.item-type-parameter-for-context";
    public static final String PROP_ITEM_TYPE_FOR_CONTEXT_DEFAULT = "Page";
    public static final String PROP_GLOBAL_WRAPPER_PAGENAME = "com.fatwire.developernet.uri.itemcontext.global-wrapper-pagename";
    public static final String PROP_GLOBAL_TEMPLATE_PAGENAME = "com.fatwire.developernet.uri.itemcontext.global-template-pagename";
    public static final String PROP_ALWAYS_UNPACK_ARGS = "com.fatwire.developernet.uri.itemcontext.nopack-args";

    private final Assembler theBackupAssembler = new QueryAssembler();
    private final Collection<String> nopack_args = new ArrayList<String>();
    private String context_type = PROP_ITEM_TYPE_FOR_CONTEXT_DEFAULT;
    private String global_wrapper_pagename;
    private String global_template_pagename;

    public void setProperties(Properties properties)
    {
        super.setProperties(properties);
        theBackupAssembler.setProperties(properties);
        nopack_args.addAll(Arrays.asList(properties.getProperty(PROP_ALWAYS_UNPACK_ARGS, "").split(",")));
        context_type = getProperty(PROP_ITEM_TYPE_FOR_CONTEXT, PROP_ITEM_TYPE_FOR_CONTEXT_DEFAULT);
        global_wrapper_pagename = getProperty(PROP_GLOBAL_WRAPPER_PAGENAME, null);
        global_template_pagename = getProperty(PROP_GLOBAL_TEMPLATE_PAGENAME, null);
        LOG.info("initializing com.fatwire.developernet.uri.itemcontext.ItemContextAssembler with properties");
    }

    public URI assemble(Definition definition) throws URISyntaxException
    {
        if(LOG.isDebugEnabled())
        {
            LOG.debug("Assembling definition: " + definition);
        }
        String scheme = definition.getScheme();
        String authority = definition.getAuthority();
        String path = _getPath(definition);
        if(path == null)
        {
            return theBackupAssembler.assemble(definition); // Can't assemble this URL.
        }
        String quotedQueryString = _getQuotedQueryString(definition);
        String fragment = definition.getFragment();
        return constructURI(scheme, authority, path, quotedQueryString, fragment);
    }

    private String _getPath(Definition definition)
    {
        if(definition.getAppType() != Definition.AppType.CONTENT_SERVER)
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("App type is not Content Server");
            }
            return null;
        }
        if(definition.getSatelliteContext() != Definition.SatelliteContext.SATELLITE_SERVER)
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("Satellite Context is not Satellite Server");
            }
            return null;
        }

        String pagename = definition.getParameter("pagename");
        String expectedWrapper = _getWrapperForAuthority(definition.getAuthority());
        if(pagename == null || !pagename.equals(expectedWrapper))
        {

            if(LOG.isTraceEnabled())
            {
                LOG.trace("Pagename not set to a valid value: " + pagename + ", expecting " + expectedWrapper);
            }
            return null;
        }

        String childpagename = definition.getParameter("childpagename");
        String expectedTemplate = _getTemplateForAuthority(definition.getAuthority());
        if(childpagename == null || !childpagename.equals(expectedTemplate))
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("Childpagename not set to a valid value: " + childpagename + ", expecting " + expectedTemplate);
            }
            return null;
        }

        String packedargs = definition.getParameter("packedargs"); // this is so annoying...
        Map<String, String[]> packed = parseQueryString(packedargs);
        String item_context = packed.get("item-context") != null ? packed.get("item-context")[0] : null;
        if(item_context == null)
        {
            item_context = definition.getParameter("item-context");
        }
        String item_alias = packed.get("item-alias") != null ? packed.get("item-alias")[0] : null;
        if(item_alias == null)
        {
            item_alias = definition.getParameter("item-alias");
        }
        String item_type = definition.getParameter("item-type");
        if(item_type == null || item_type.length() == 0)
        {
            item_type = definition.getParameter("c"); // backup plan
        }
        String variant = packed.get("variant") != null ? packed.get("variant")[0] : null;
        if(variant == null)
        {
            variant = definition.getParameter("variant");
        }


        if(item_context == null || item_alias == null || item_type == null)
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("Could not assemble URL because item-type, item-context or item-alias was not valid: (" + item_type + "), (" + item_context + "), (" + item_alias + ")");
            }
            return null; // Can't assemble this URL. Sorry...
        }

        // content server bug does not handle decoding names with spaces very well.
        if(item_alias.indexOf(" ") >= 0 || item_context.indexOf(" ") >= 0)
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("Could not assemble URL because item-alias and/or item-context has a space in it, and a CS bug prevents proper decoding of encoded spaces in URLs.  (" + item_type + "), (" + item_context + "), (" + item_alias + ")");
            }
            return null;
        }


        StringBuilder path = new StringBuilder(getProperty(PROP_URIBASE_SATELLITE_SERVER, null));
        if(!path.toString().endsWith("/"))
        {
            path.append('/');
        }
        path.append(item_context);
        if(!context_type.equals(item_type) && !item_context.endsWith(item_alias))
        {
            // we do not want to duplicate the page name in both item-context and cname
            path.append('/').append(aliasFromItemTypeName(item_type));
            path.append('/').append(item_alias);
        }
        if(variant != null)
        {
            path.append("/v").append(variant);
        }
        return path.toString();
    }

    /**
     * List of parameters that are effectively embedded in the pathinfo for this URL.
     * item-type is embedded as itself, p is embedded as item-context, cid is embedded as item-alias.
     */
    private static List<String> EMBEDDED_PARAMS = Arrays.asList("pagename", "childpagename", "item-context", "item-alias", "variant", "item-type", "c", "cid", "p");

    private String _getQuotedQueryString(Definition definition)
    {
        Map<String, String[]> newQryParams = new HashMap<String, String[]>();

        // build the query string if there is one
        for(Object o : definition.getParameterNames())
        {
            // Get the parameter name
            String key = (String)o;

            // don't add embedded params to the query string, and strip embedded params from packedargs
            if(!EMBEDDED_PARAMS.contains(key))
            {
                String[] vals = definition.getParameters(key);

                if(key.equals("packedargs"))
                {
                    Collection<String> exclude = EMBEDDED_PARAMS;

                    // if some params need to be extracted from packedargs but still need to stay in the query string
                    // process those now
                    if(nopack_args.size() > 0)
                    {
                        exclude = new ArrayList<String>();
                        exclude.addAll(EMBEDDED_PARAMS);

                        Map<String, String[]> packed = parseQueryString(vals[0]);
                        for(String nopack_key : nopack_args)
                        {
                            String[] nopack_val = packed.get(nopack_key);
                            if(nopack_val != null)
                            {
                                newQryParams.put(nopack_key, nopack_val);
                                exclude.add(nopack_key);
                            }
                        }
                    }

                    vals = _excludeFromPackedargs(vals, exclude);
                }
                newQryParams.put(key, vals);
            }
        }
        return _toQueryString(newQryParams);
    }

    private String _toQueryString(Map<String, String[]> newQryParams)
    {
        StringBuilder qryStr = new StringBuilder();
        for(String key : newQryParams.keySet())
        {
            String[] vals = newQryParams.get(key);
            if(vals != null)
            {
                // Loop through the values for the parameter
                for(String val : vals)
                {
                    // Append the correct separator
                    if(qryStr.length() > 0)
                    {
                        qryStr.append('&');
                    }

                    // Append the name and value to the URL
                    if(LOG.isTraceEnabled())
                    {
                        StringBuilder bf = new StringBuilder("About to add [key]=[value] to url [" + key + "]=[" + val + "]");
                        bf.append(" after encoding: [").append(encode(key)).append("]=[").append(encode(val)).append("]");
                        LOG.trace(bf);

                    }
                    qryStr.append(encode(key)).append('=').append(encode(val));
                }
            }
        }

        // prepare result
        if(qryStr.length() > 0)
        {
            return qryStr.toString();
        }
        else
        {
            return null;
        }
    }

    private String[] _excludeFromPackedargs(String[] vals, Collection<String> toExclude)
    {
        Map<String, String[]> oldPacked = parseQueryString(vals[0]);
        Map<String, String[]> newPacked = new HashMap<String, String[]>();
        for(String opK : oldPacked.keySet())
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("checking to see if a param should be excluded from packedargs: " + opK);
            }
            if(!toExclude.contains(opK))
            {
                newPacked.put(opK, oldPacked.get(opK));
            }
        }
        vals = null;
        if(newPacked.size() > 0)
        {
            StringBuilder newPackedStr = new StringBuilder();
            for(String npK : newPacked.keySet())
            {
                for(String npV : newPacked.get(npK))
                {
                    if(newPackedStr.length() > 0)
                    {
                        newPackedStr.append('&');
                    }
                    newPackedStr.append(encode(npK)).append('=').append(encode(npV));
                }
            }
            vals = new String[1];
            vals[0] = newPackedStr.toString();
        }
        return vals;
    }

    public Definition disassemble(URI uri, ContainerType containerType) throws URISyntaxException
    {
        if(LOG.isDebugEnabled())
        {
            LOG.debug("Disassembling URI: " + uri);
        }

        Simple result;
        try
        {
            final Map qryParams = getQueryParams(uri); // this is the main workhorse function.
            if(qryParams == null)
            {
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("Attempted to disassemble: " + uri + " " + this.getClass().getName() + " but the URL was not recognized.  No further attempt to decode this URL with this assembler will be made");
                }
                // This URL was not recognized and cannot be decoded.  Stop trying to deal with it.
                return theBackupAssembler.disassemble(uri, containerType);
            }
            final Definition.AppType appType = Definition.AppType.CONTENT_SERVER;
            final Definition.SatelliteContext satelliteContext = Definition.SatelliteContext.SATELLITE_SERVER;
            final boolean sessionEncode = false;
            final String scheme = uri.getScheme();
            final String authority = uri.getAuthority();
            final String fragment = uri.getFragment();

            result = new Simple(sessionEncode, satelliteContext, containerType, scheme, authority, appType, fragment);
            result.setQueryStringParameters(qryParams);
        }
        catch(IllegalArgumentException e)
        {
            // Something bad happened
            throw new URISyntaxException(uri.toString(), e.toString());
        }

        return result;
    }

    protected Map<String, String[]> getQueryParams(URI uri)
    {
        String uripath = uri.getPath();
        if(uripath == null)
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("No path found in URI: " + uri);
            }
            return null;
        }

        // path is of the form /<base>/<item-context>/<item-type>/<item-alias>/v<variant>
        // or
        // path is of the form /<base>/<item-context>/<item-type>/<item-alias>
        // or
        // path is of the form /<base>/<item-context>/v<variant>
        // or
        // path is of the form /<base>/<item-context>

        String pathPrefix = getProperty(PROP_URIBASE_SATELLITE_SERVER, null);

        if(!uripath.startsWith(pathPrefix))
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("Path does not start with expected value in URI: " + uri + ".  expected:" + pathPrefix);
            }
            return null;
        }

        String uripathNoBase = uripath.substring(pathPrefix.length());
        String[] pathElements = uripathNoBase.split("/");
        if(pathElements.length < 1)
        {
            LOG.trace("not enough path elements in uri: " + uri + ".  expected:" + pathPrefix);
            return null;
        }

        // start by parsing the query string
        Map<String, String[]> params = parseQueryString(uri.getRawQuery());

        for(String param : EMBEDDED_PARAMS)
        {
            if(params.containsKey(param))
            {
                LOG.trace("found a param in the URL that should be embedded: " + param);
                return null;
            }
        }


        // parse params
        List<String> path = new ArrayList<String>(Arrays.asList(pathElements));

        String variant = _getVariant(path);
        if(variant != null)
        {
            String[] s = {variant};
            LOG.trace("Variant decoded to: " + variant);
            params.put("variant", s);
        }

        String[] cAndCpath = _getCandCpath(path);
        if(cAndCpath != null)
        {
            String[] item_type = {cAndCpath[0]};
            String[] item_alias = {cAndCpath[1]};
            params.put("item-type", item_type);
            params.put("c", item_type); // for simplicity
            params.put("item-alias", item_alias);
            LOG.trace("item-type decoded to:" + item_type[0]);
            LOG.trace("item-alias decoded to:" + item_alias[0]);
        }

        String item_context = _getCpath(path);
        String[] saPpath = {item_context};
        params.put("item-context", saPpath);
        LOG.trace("item-context decoded to:" + item_context);

        if(cAndCpath == null)
        {
            String[] item_type = {context_type};
            String[] item_alias = {path.get(path.size() - 1)}; // path has been trimmed by now
            params.put("item-type", item_type);
            params.put("c", item_type); // for simplicity
            params.put("item-alias", item_alias);
            LOG.trace("item-type decoded to:" + item_type[0]);
            LOG.trace("item-alias decoded to:" + item_alias[0]);
        }


        // less interesting params now

        String[] layoutTemplate = {_getTemplateForAuthority(uri.getAuthority())};
        params.put("childpagename", layoutTemplate);
        if(LOG.isTraceEnabled())
        {
            LOG.trace("childpagename decoded to " + layoutTemplate[0]);
        }

        String[] wrapper = {_getWrapperForAuthority(uri.getAuthority())};
        params.put("pagename", wrapper);
        if(LOG.isTraceEnabled())
        {
            LOG.trace("pagename decoded to " + wrapper[0]);
        }

        return params;
    }

    private String _getVariant(List<String> path)
    {
        if(path.size() >= 2)
        {
            String s = path.get(path.size() - 1);

            if(s != null && s.length() > 1 && s.startsWith("v"))
            {
                s = s.substring(1);
                try
                {
                    Integer.parseInt(s);
                    // found it!  remove the extra entry from the path list
                    path.remove(path.size() - 1);
                    return s;
                }
                catch(Exception e)
                {
                    // no path
                }
            }
        }
        return null;

    }

    private String[] _getCandCpath(List<String> path)
    {
        if(path.size() >= 3)
        {
            String cAliasCandidate = path.get(path.size() - 2);
            String cCandidate = itemTypeNameFromAlias(cAliasCandidate);
            if(cCandidate != null)
            {
                String[] s = new String[2];
                s[0] = cCandidate;
                s[1] = path.get(path.size() - 1);
                path.remove(path.size() - 1);
                path.remove(path.size() - 1);
                return s;
            }
        }
        return null;
    }

    private String _getCpath(List<String> path)
    {
        // the rest is ours
        StringBuilder s = new StringBuilder();
        for(String element : path)
        {
            if(s.length() > 0)
            {
                s.append("/");
            }
            s.append(element);
        }
        return s.toString();
    }


    private String itemTypeNameFromAlias(String alias)
    {
        return getProperty(PROP_ITEM_TYPE_ALIAS_PREFIX + alias, null);
    }

    private String aliasFromItemTypeName(String atName)
    {
        return getProperty(PROP_ITEM_TYPE_PARAMETER_PREFIX + atName, null);
    }

    private final String _getWrapperForAuthority(String authority)
    {
        // TODO: figure out how to let this be more configuratble.
        // Using the authority from the URL did not quite work because
        // for some links, the authority comes back blank, which would
        // be ambiguous in a multi-site environment.  For now, don't
        // expose this feature until it can be much more robust
        return global_wrapper_pagename;
    }

    private final String _getTemplateForAuthority(String authority)
    {
        // TODO: figure out how to let this be more configuratble.
        // Using the authority from the URL did not quite work because
        // for some links, the authority comes back blank, which would
        // be ambiguous in a multi-site environment.  For now, don't
        // expose this feature until it can be much more robust
        return global_template_pagename;
    }

}
