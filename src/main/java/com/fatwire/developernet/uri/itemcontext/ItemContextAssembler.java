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
import com.fatwire.developernet.uri.itemcontext.aliasing.AssetAliasingStrategy;
import com.fatwire.developernet.uri.lightweight.LightweightAbstractAssembler;
import com.fatwire.developernet.uri.siteplan.Helper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * <p>Generalized URL Assembler useful for converting an item of a specific type and its context into a
 * folder-like, easy-to-read URL and back.  It intentionally does not intend to make any assumptions about
 * what constitutes the context or the item placed in that context.  Mapping these abstract entities into
 * concrete objects used by the site implementation is the responsibility of the user.  Often translation
 * helper libraries may be utilized for this purpose.  See {@link com.fatwire.developernet.uri.itemcontext.aliasing}
 * and {@link Helper} for examples.  This assembler supports automatic unpacking of arguments from packedargs,
 * automatic detection of required parameters in packedargs, integer <code>variant</code> identifiers for
 * multivariate testing, and asset type aliasing, all in addition to the specified core features.</p>
 * <p/>
 * <p>In this assembler, URLs have the following form:
 * <code>{protocol}://{domain}/{context path}/{servlet name}/{item-context}/{item-type}/{item-alias}/v{variant number}</code>.
 * </p>
 * <p>Generally, this means that an individual item and its context can be represented in the URI.
 * If the item itself is not required, its type and identifier can both be suppressed from the URI.
 * In addition, a numerical variant can also be suppressed.  Thus, the assembler can be written
 * as follows:
 * <code>[{protocol}://{domain}]/{context path}/{servlet name}/{item-context}[/{item-type}/{item-alias}][/v{variant number}]</code>.
 * where [] denote optional components.</p>
 * <p>It is also possible that {context path} and {servlet name} can be compressed out of existence, but this is a
 * server configuration matter and is not addressed in this document.</p>
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
 * <li>http://localhost/cm/company/media/press-kit/policies/logo-full (In this example, the path to the asset
 * in the site plan tree is <code>company/media/press-kit</code>, and thus that denotes the {item-context} for the URL.
 * It is defined by the variable <code>p</code>.  The value of <code>p</code> would be the id of the
 * <code>press-kit</code> page placed under the <code>media</code> page..  The asset type alias is <code>policies</code>
 * which will probably map to an asset type like <code>Policy</code> or <code>Article</code>, corresponding to the
 * variable <code>c</code>.  Finally, the {item-alias} parameter maps to <code>logo-full</code> which maps to an
 * individual asset, corresponding to the <code>cid</code> variable.</li>
 * <li>http://localhost/cs/Satellite/brand-x/catalogue/electronics/audio-players/ipod/v2</li>
 * </ul>
 * <p>It is the intent that this assembler could be used to assemble any URL that utilizes
 * the site plan tree and an asset id, that is, <code>c</code>, <code>cid</code>, and <code>p</code>
 * to uniquely identify every page on the site.  It was also designed with support for multi-element
 * (i.e. multiple slashes) values for both {item-context} and {item-alias}.  However, only
 * a single path element is permitted for {item-type}.</p>
 * <p>As described above, this assembler is typically used in conjunction with a helper class that is called from
 * within Content Server to calculate {item-context} and {item-alias} when links are being
 * created, as well as to resolve the original source variables (like <code>c</code>,
 * <code>cid</code>, and <code>p</code> for example) when the page is parsed.  If the
 * FirstSiteII rendering model or a related rendering model is being used, this typically
 * means that the computation of {item-context} and {item-alias} is done in the <code>Link</code>
 * templates, and the decoding is done in the <code>Wrapper</code> templates.  FirstSiteII and BurlingtonFinancial
 * rendering model sites can make use of teh {@link Helper} class which
 * does this mapping bi-directionally, with multiple, pluggable aliasing strategies.</p>
 * <p>If decoding happens in a template that is uncached, it may be required for the decoder
 * to have built-in caching support.</p>
 * <p/>
 * <p><strong>What is Not Supported</strong></p>
 * <p>This assembler does NOT support BlobServer URLs using the structure above.  BlobServer URLs are delegated
 * to a fallback assembler which can be configured.  The default fallback assembler is the {@link QueryAssembler}.</p>
 * <p>This assembler does not support direct rendering through Content Server.  Only Satellite Server URLs will be
 * properly decoded.  If a URL requiring the Content Server satellite context is passed to the {@link #assemble} method,
 * this assembler will delegate decoding to the fallback assembler.</p>
 * <p/>
 * <p><strong>Requirements</strong></p>
 * <p>For definitions to be assembled into URLs, the following requirements apply:</p>
 * <ul>
 * <li>item-context must be set</li>
 * <li>item-type or c must be set</li>
 * <li>item-alias must be set</li>
 * <li>SatelliteContext must be set to Satellite Server</li>
 * <li>AppType must be set to Content Server</li>
 * <li>pagename must be set to the value corresponding to the {@link #PROP_GLOBAL_WRAPPER_PAGENAME} property</li>
 * <li>childpagename must be set to the value corresponding to the {@link #PROP_GLOBAL_TEMPLATE_PAGENAME} property</li>
 * <li>Spaces are not present in any of item-context, item-type, or item-alias</li>
 * </ul>
 * <p>As indicated above, if item-type is not specified, it is possible to use the variable <code>c</code> instead.
 * Upon disassembly, both item-type and c will be re-populated.</p>
 * <p>In addition, item-context, item-type, item-alias and variant will be detected if present as independent parameters
 * or if they are encoded in the packedargs parameter.</p>
 * <p/>
 * <p>For URIs to be decoded properly into Definitions, the following requirements apply:</p>
 * <ul>
 * <li>The URL path must start with the value of the {@link #PROP_URIBASE_SATELLITE_SERVER} property</li>
 * <li>The path must contain at least one path element</li>
 * <li>None of the parameters that are normally embedded in the URL may be present as query string parameters</li>
 * </ul>
 * <p>If item-alias and item-type are omitted from the URL, the last context element is used as the item-alias, and
 * the item-type is set to the value of the {@link #PROP_ITEM_TYPE_FOR_CONTEXT} property, which defaults to a value
 * of {@link #PROP_ITEM_TYPE_FOR_CONTEXT_DEFAULT}.</p>
 * <p/>
 * <p><strong>Configuration</strong></p>
 * <p>This assembler depends on some main configuration properties.  Content Server version 7 requires these properties
 * to be set in the ServletRequest.properties file.</p>
 * <p>The property {@link #PROP_SERVLET_CONTEXT_TOGGLE} governs whether the assembler outputs the servlet context and
 * servlet name during assembly.</p>
 * <p>The item type aliasing needs to be  defined for each item type to be converted by this assembler.  See
 * {@link #PROP_ITEM_TYPE_PARAMETER_PREFIX} and {@link #PROP_ITEM_TYPE_ALIAS_PREFIX} for details. </p>
 * <p>Item type for contexts must also be configured if the default value is not desired.
 * See {@link #PROP_ITEM_TYPE_FOR_CONTEXT} for details.</p>
 * <p>The required template name and wrapper name also need to be configured.  This assembler will only be executed if
 * these parameters are set to the configured values.  See {@link #PROP_GLOBAL_WRAPPER_PAGENAME} and
 * {@link #PROP_GLOBAL_TEMPLATE_PAGENAME} for details.</p>
 * <p>The list of parameters to always be unpacked from packedargs can optionally be configured.  See
 * {@link #PROP_ALWAYS_UNPACK_ARGS} for details.
 *
 * @author Tony Field
 * @author Matthew Soh
 * @since May 14, 2009
 */
public final class ItemContextAssembler extends LightweightAbstractAssembler
{
    private static final Log LOG = LogFactory.getLog("com.fatwire.developernet.uri.itemcontext");

    /**
     * <p>Prefix of the property used to define which alias is to be used by the item type appended to this prefix.</p>
     * <p>e.g. com.fatwire.developernet.uri.itemcontext.item-type.parameter.FW_Content_C = article</p>
     * <p>This is required for any asset type that is to be enabled..</p>
     */
    public static final String PROP_ITEM_TYPE_PARAMETER_PREFIX = "com.fatwire.developernet.uri.itemcontext.item-type.parameter.";

    /**
     * <p>Prefix of the property used to define which item type to use for the item alias appended to this prefix.</p>
     * <p>e.g. com.fatwire.developernet.uri.itemcontext.item-type.alias.article = FW_Content_C</p>
     * <p>This is required for any asset type that is to be enabled..</p>
     */
    public static final String PROP_ITEM_TYPE_ALIAS_PREFIX = "com.fatwire.developernet.uri.itemcontext.item-type.alias.";

    /**
     * Property defining the item type to be used when no item alias is found in the URL's pathinfo.  The last element
     * of the context is used as the item-alias and this property defines which item-type to use.
     *
     * @see #PROP_ITEM_TYPE_FOR_CONTEXT_DEFAULT
     */
    public static final String PROP_ITEM_TYPE_FOR_CONTEXT = "com.fatwire.developernet.uri.itemcontext.item-type-parameter-for-context";

    /**
     * Default value of the item type to use when item-alias is not found in pathinfo.  Default value is Page.
     *
     * @see #PROP_ITEM_TYPE_FOR_CONTEXT
     */
    public static final String PROP_ITEM_TYPE_FOR_CONTEXT_DEFAULT = "Page";

    /**
     * Property defining the required value of Pagename to be used if this assembler is to be used.
     */
    public static final String PROP_GLOBAL_WRAPPER_PAGENAME = "com.fatwire.developernet.uri.itemcontext.global-wrapper-pagename";

    /**
     * Property defining the requried value of the template's pagename to be used if this assembler is to be used.
     */
    public static final String PROP_GLOBAL_TEMPLATE_PAGENAME = "com.fatwire.developernet.uri.itemcontext.global-template-pagename";

    /**
     * Comma-separated list of variable names to be always removed from packedargs and displayed as query string
     * parameters.  This helps remove packedargs from the query string.  No default values are ever set.
     */
    public static final String PROP_ALWAYS_UNPACK_ARGS = "com.fatwire.developernet.uri.itemcontext.nopack-args";


    /**
     * Indicates whether to append the servlet context and servlet name to the URL generated.
     */
    public static final String PROP_SERVLET_CONTEXT_TOGGLE = "com.fatwire.developernet.uri.itemcontext.append-servlet-context";

    private final Assembler theBackupAssembler = new QueryAssembler(); // todo: consider configuring this (must be backward-compatible though)
    private final Collection<String> nopack_args = new ArrayList<String>();
    private String context_type = PROP_ITEM_TYPE_FOR_CONTEXT_DEFAULT;
    private String global_wrapper_pagename;
    private String global_template_pagename;
    private boolean append_servlet_context;

    public void setProperties(Properties properties)
    {
        super.setProperties(properties);
        theBackupAssembler.setProperties(properties);
        nopack_args.addAll(Arrays.asList(properties.getProperty(PROP_ALWAYS_UNPACK_ARGS, "").split(",")));
        context_type = getProperty(PROP_ITEM_TYPE_FOR_CONTEXT, PROP_ITEM_TYPE_FOR_CONTEXT_DEFAULT);
        global_wrapper_pagename = getProperty(PROP_GLOBAL_WRAPPER_PAGENAME, null);
        global_template_pagename = getProperty(PROP_GLOBAL_TEMPLATE_PAGENAME, null);
        append_servlet_context = Boolean.valueOf(getProperty(PROP_SERVLET_CONTEXT_TOGGLE, "true")).booleanValue();
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

    /**
     * Main worker function for assembly of the URL.
     *
     * @param definition input definition
     * @return path for URL
     */
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


        if(item_context == null || item_context.length() == 0
           || item_alias == null || item_alias.length() == 0
           || item_type == null || item_type.length() == 0)
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("Could not assemble URL because item-type, item-context or item-alias was not valid: (" + item_type + "), (" + item_context + "), (" + item_alias + ")");
            }
            return null; // Can't assemble this URL. Sorry...
        }

        // content server bug does not handle decoding names with spaces very well.

        if(AssetAliasingStrategy.ILLEGAL_CHARACTER_PATTERN.matcher(item_alias).find())
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("Could not assemble URL because item-alias contains illegal characters.  (" + item_type + "), (" + item_context + "), (" + item_alias + ")");
            }
            return null;
        }

        StringBuilder path = new StringBuilder();
        if(append_servlet_context)
        {
            path.append(getProperty(PROP_URIBASE_SATELLITE_SERVER, ""));
        }

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

                    vals = excludeFromPackedargs(vals, exclude);
                }
                newQryParams.put(key, vals);
            }
        }
        return constructQueryString(newQryParams);
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
            final Map<String, String[]> qryParams = getQueryParams(uri); // this is the main workhorse function.
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

    /**
     * Main worker function for URL disassembly.  Parses path and adds params back into the list of parameters.
     *
     * @param uri input URI
     * @return may of parameters
     */
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

        if(uripath.equals(pathPrefix))
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("This looks like a regular QueryString assembler request. Not processing further.");
            }
            return null;
        }

        String uripathNoBase = uripath.substring(pathPrefix.length() + 1);  // +1 for the leading slash
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


    /**
     * Given an item alias, return the item type.
     *
     * @param alias input alias
     * @return item type or null if not configured
     */
    private String itemTypeNameFromAlias(String alias)
    {
        return getProperty(PROP_ITEM_TYPE_ALIAS_PREFIX + alias, null);
    }

    /**
     * Given an item type, return its alias
     *
     * @param atName item type name
     * @return alias or null if not configured
     */
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
