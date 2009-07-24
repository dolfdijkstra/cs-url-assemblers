/*
 * Copyright (c) 2009 FatWire Corporation. All Rights Reserved.
 * Title, ownership rights, and intellectual property rights in and
 * to this software remain with FatWire Corporation. This  software
 * is protected by international copyright laws and treaties, and
 * may be protected by other law.  Violation of copyright laws may
 * result in civil liability and criminal penalties.
 */
package com.fatwire.developernet.uri.siteplan;

import COM.FutureTense.Interfaces.*;
import COM.FutureTense.Util.IterableIListWrapper;
import COM.FutureTense.Util.ftErrors;
import com.fatwire.assetapi.data.AssetId;
import com.fatwire.developernet.CSRuntimeException;
import static com.fatwire.developernet.IListUtils.getLongValue;
import static com.fatwire.developernet.IListUtils.getStringValue;
import com.fatwire.developernet.facade.runtag.example.asset.Children;
import com.fatwire.developernet.facade.runtag.example.siteplan.NodePath;
import com.fatwire.developernet.uri.itemcontext.aliasing.*;
import com.fatwire.mda.Dimension;
import com.openmarket.xcelerate.asset.AssetIdImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * <p>Helper class for dealing with the ItemContext assembler that is based on the Site Plan Tree.  Specifically,
 * When the context is defined as a concatenation of sequential page aliases in the Site plan tree, this assembler
 * is appropriate.  In other words, <code>item-context</code> can be mapped to
 * <code>[level1page]/[level2page]/[level3page]</code>.
 * This can result in URLs that look like the following:
 * <code>http://localhost:8080/cs/Satellite/[level1page]/[level2page]/[level3page]<code>
 * <code>http://localhost:8080/cs/Satellite/[level1page]/[level2page]/[level3page]/article/privacy-policy<code>
 * <code>http://localhost:8080/cs/Satellite/[level1page]/[level2page]/[level3page]/article/privacy-policy/v2<code>
 * for example.</p>
 * <p/>
 * <p>To compute the value of the item-alias and context, aliases must be computed for both content assets and page
 * assets.  The helper is able to rely heavily on a single aliasing interface to convert between an input asset ID
 * and an alias, and back from an alias to a list of possible candidate asset IDs.  Without constraining aliases, it is
 * not possible to deterministically dereference an asset ID from an input alias.  Constraining aliases is not desired.
 * While this opens the possibility that some aliases can never be resolved, the combination of aliases and context
 * make it practically impossible.  The only way an alias could not be resolved when used in context is when two
 * identical aliases exist in the same context.  While a theoretical possibility, when used for URLs, this situation
 * is illogical and should not occur.  If this situation is detected, this helper class simply guesses that the first
 * alias found is "good enough" and thus it will be returned.  A warning message is logged. See
 * {@link AssetAliasingStrategy} for details about how asset aliasing works.</p>
 * <p/>
 * <p><strong>Configuration</strong></p>
 * <p>No properties are required for this assembler to work.  Default values are set for all configuration properties.
 * However, there are two key properties that should be set in practical cases:</p>
 * <p>The property {@link #PROPERTY_MAX_DEPTH_PROP_NAME} defines how many levels of navigation placeholders are present
 * in the Site Plan Tree, so that some pages can be excluded.  The default value of 1 allows a single tier of
 * placeholders.</p>
 * <p>The property {@link #PROPERTY_ALIASING_STRATEGY} defines which aliasing strategy to use.  Depending on the asset
 * model, a variety of strategies can be employed.  If a desired strategy is not already implemented, it is fairly
 * straight-forward to define custom strategies.  The default strategy, defined in
 * {@link #PROPERTY_ALIASING_STRATEGY_DEFAULT} simply uses the asset's ID as its alias.  As the most trivial
 * implementation, it is not particularly compelling for an actual deployment.  The following list describes some of the
 * strategies that ship with this assembler helper</p>
 * <ul>
 * <li>{@link IdAliasingStrategy} uses the asset's ID only</li>
 * <li>{@link NameAliasingStrategy} uses the asset's name.  Will always generate an alias but may not be usable in
 * the assembler if spaces are found</li>
 * <li>{@link PathAliasingStrategy} uses the asset's path.  Path is not a required field in assets to this may not
 * work without setting values in the asset.  This is, however, the simplest clean solution.</li>
 * <li>{@link PageMetadataArticleAliasingStrategy} uses the path attribute from an article associated to page assets,
 * or uses the path attribute for other asset types.</li>
 * <li>{@link MultilingualPageMetadataArticleAliasingStrategy} uses hte path attribute of an article associated
 * to the specified page asset, and then translates the article into the locale specified.</li>
 * </ul>
 * <p/>
 * <p><strong>Usage</strong></p>
 * <p>This helper class needs to be used in two places - when URLs are first created, and when URLs are decomposed and
 * converted into variables.  In Content Server, URLs are created using the <code>render:gettemplateurl</code> tag.</p>
 * <p>If the functionality of this class could be integrated into the tag, it could be called automatically.  However,
 * since it is not the case, it has to be explicitly invoked.  The best place to do this, according to the FirstSite
 * II rendering model, is in each asset type's <code>Link</code> template.  Typically, it will be called like this:</p>
 * <code>
 * [% Helper spHelper = new Helper(ics); %]
 * [render:gettemplateurl tname="/Layout" wrapper="Wrapper" c='[ics:getvar("c")]' cid='[ics.getvar("cid")]' ... etc ]
 * [render:argument name='p' value='[ics.GetVar("p")]' /]
 * [render:argument name='item-context' value='[spHelper.computeItemContext(Long.valueOf(ics.GetVar("p")), "en_US")]'/]
 * [render:argumane name='item-alias' value='[spHelper.computeAlias(ics.GetVar("c"), ics.GetVar("cid"), "en_US")]'/]
 * [/render:gettemplateurl]
 * </code>
 * <p>URLs are automatically decomposed by Content Server by calling the URL Assembler.  However, it is important that
 * templates convert item-alias and item-context into cid and p so that they can be used programmatically in various
 * situations.  One place where this can be done easily is in the wrapper JSP or controller.  It should be noted that
 * if the wrapper is uncached, some performance degradation will ensue if the alias resolving process accesses the
 * database, and this needs to be mitigated.  There are various methods of achieving this but they are not discussed
 * in this document.  Typical usage of cid/p resolution looks like this - often at the very top of the wrapper:</p>
 * <code>
 * String itemcontext = ics.GetVar("item-context");
 * String itemalias = ics.GetVar("item-alias");
 * String c = ics.GetVar("item-type");
 * long longP = -1;
 * long longCid = -1;
 * <p/>
 * Helper helper = new Helper(ics);
 * <p/>
 * if (itemcontext != null) {
 * longP = helper.resolvePForItemContext(itemcontext);
 * if (ics.GetVar("p") != null) {
 * log.warn("Unexpected value of p found in conjunction with itemcontext:"+ics.GetVar("p")+", "+ics.GetVar("item-context")+".  Ignoring item-context.");
 * }
 * else {
 * log.debug("Setting decoded P from item-context: "+itemcontext);
 * ics.SetVar("p", Long.toString(longP));
 * }
 * }
 * if (itemalias != null) {
 * longCid = helper.resolveCidFromAlias(c, itemalias, longP);
 * if (ics.GetVar("cid") != null) {
 * log.warn("Unexpected value of cid found in conjunction with itemalias:"+ics.GetVar("cid")+", "+ics.GetVar("item-alias")+".  Ignoring itemalias.");
 * }
 * else {
 * log.debug("Setting decoded cid from item-alias: "+itemalias);
 * ics.SetVar("cid", Long.toString(longCid));
 * }
 * }
 * </code>
 *
 * @author Tony Field
 * @since Jun 1, 2009
 */
public class Helper
{
    private static final Log LOG = LogFactory.getLog("com.fatwire.developernet.uri.siteplan.helper");

    /**
     * Name of configuration file.  If configuration properties are not in this
     * file, Java System properties will be checked.  the file must be in the classpath
     * or in the inipath.
     */
    public static final String CONFIGURATION_FILE_NAME = "ServletRequest.properties";

    /**
     * Property specifying the lowest level (i.e. closest to the root) placed page
     * in the site plan to be used when calculating ppath and when resolving ppath.
     * This property is 0-based, so if you want to include all SitePlan pages,
     * set the property to 0.
     *
     * @see #PROPERTY_MAX_DEPTH_PROP_NAME_DEFAULT
     */
    public static final String PROPERTY_MAX_DEPTH_PROP_NAME = "com.fatwire.developernet.uri.siteplan.helper.lowest-level-to-include";

    /**
     * The default value of the {@link #PROPERTY_MAX_DEPTH_PROP_NAME} if not otherwise specified.
     * The default value is 1, which supports a single level of navigation placeholder pages
     * in the SitePlanTree - the most common configuration.
     *
     * @see #PROPERTY_MAX_DEPTH_PROP_NAME
     */
    public static final String PROPERTY_MAX_DEPTH_PROP_NAME_DEFAULT = "1";

    /**
     * Property specifying the asset aliasing strategy to use.  Specify the classname for the strategy.
     *
     * @see #PROPERTY_ALIASING_STRATEGY_DEFAULT
     */
    public static final String PROPERTY_ALIASING_STRATEGY = "com.fatwire.developernet.uri.siteplan.helper.aliasing-strategy-class";

    /**
     * Default value for the {@link #PROPERTY_ALIASING_STRATEGY} property.
     */
    public static final String PROPERTY_ALIASING_STRATEGY_DEFAULT = IdAliasingStrategy.class.getName();

    private final int lowestLevelToInclude;
    private final ICS ics;
    private final AssetAliasingStrategy translator;

    /**
     * Helper class for looking up cpath and ppath in URLs.  This class contains a reference to the ICS context and
     * must be released prior to the destruction of the ICS object.  Typically, instantiating a new instance for each
     * JSP is sufficient.  In the case of a reusable controller, it should be re-instantiated on each request.
     *
     * @param ics context.
     */
    public Helper(ICS ics)
    {
        this.ics = ics;
        lowestLevelToInclude = Integer.parseInt(_getProperty(PROPERTY_MAX_DEPTH_PROP_NAME, PROPERTY_MAX_DEPTH_PROP_NAME_DEFAULT));
        translator = _getAliasingStrategy(_getProperty(PROPERTY_ALIASING_STRATEGY, PROPERTY_ALIASING_STRATEGY_DEFAULT), ics);
    }

    /**
     * Instantiate the aliasing strategy
     *
     * @param clazz class name to instantiate
     * @param ics context
     * @return strategy
     */
    private AssetAliasingStrategy _getAliasingStrategy(String clazz, ICS ics)
    {
        Class c;
        Constructor con;
        Object o;

        try
        {c = Class.forName(clazz);}
        catch(ClassNotFoundException e)
        {
            throw new CSRuntimeException("Could not find class for AssetAliasingStrategy.", ftErrors.exceptionerr, e);
        }
        try
        {con = c.getConstructor(ICS.class);}
        catch(NoSuchMethodException e)
        {
            throw new CSRuntimeException("Class " + clazz + " does not have a suitable constructor.", ftErrors.exceptionerr, e);
        }
        try
        {o = con.newInstance(ics);}
        catch(InstantiationException e)
        {
            throw new CSRuntimeException("Could not instantiate " + clazz, ftErrors.exceptionerr, e);
        }
        catch(IllegalAccessException e)
        {
            throw new CSRuntimeException("Illegal access attempting to instantiate " + clazz, ftErrors.exceptionerr, e);
        }
        catch(InvocationTargetException e)
        {
            throw new CSRuntimeException("Attempted to instantiate " + clazz + " but the constructor threw an exception: " + e.getTargetException(), ftErrors.exceptionerr, e);
        }
        AssetAliasingStrategy aas = (AssetAliasingStrategy)o;
        if(LOG.isDebugEnabled())
        {
            LOG.debug("Successfully instantiated AssetAliasingStrategy: " + clazz + ": " + aas);
        }
        return aas;
    }

    private String _getProperty(String name, String dephault)
    {
        String s = ics.GetProperty(name, CONFIGURATION_FILE_NAME, true);
        if(!Utilities.goodString(s))
        {
            s = System.getProperty(name, dephault);
        }
        return s;
    }

    /**
     * Given an input c/cid pair, compute (/look up) the alias for that asset.
     * <p/>
     * This function is not to be called from within the assembler, but within
     * Content Server.
     * <p/>
     * The return value may be null, indicating that no alias exists.
     *
     * @param c asset type
     * @param cid asset id
     * @param localeName name of the locale for which the alias should be computed
     * @return alias for the asset, or else null
     */
    public String computeAlias(String c, long cid, String localeName)
    {
        return computeAlias(new AssetIdImpl(c, cid), localeName);
    }

    public String computeAlias(AssetId id, final String localeName)
    {
        if(LOG.isDebugEnabled())
        {
            LOG.debug("computeAlias: Computing cpath for asset-locale:" + id + "-" + localeName);
        }
        String result = translator.computeAlias(id, localeName);
        if(LOG.isDebugEnabled())
        {
            LOG.debug("computeAlias: Computed cpath for asset-locale:" + id + "-" + localeName + " and got: " + result);
        }
        return result;
    }


    /**
     * Given an input page asset id, create a breadcrumb-like path structure
     * for it.  If any page asset does not have an alias specified, no context is returned.
     * <p/>
     * Note that the value for each individual element is computed using the {@link #computeAlias}
     * method defined above.  This allows for the alias attribute to be provided in various ways,
     * such as by looking up the "path" field or by looking up the "path" field of an associated asset.
     *
     * @param p page id
     * @param localeName name of locale (like en_US)
     * @return context or null
     */
    public String computeItemContext(long p, final String localeName)
    {
        if(LOG.isDebugEnabled())
        {
            LOG.debug("computeItemContext: Attempting to calculate ppath for Page:" + p + "-" + localeName + " with lowest level to include set to " + lowestLevelToInclude);
        }

        IList nodepath = NodePath.getNodePathForPage(ics, p);

        List<AssetId> trimmedBreadcrumbPath = _pruneNodePathAndReverse(nodepath);
        trimmedBreadcrumbPath.add(new AssetIdImpl("Page", p)); // add self, since it does not appear in NodePath

        String ppath = _listToString(trimmedBreadcrumbPath, localeName);

        if(LOG.isDebugEnabled())
        {
            LOG.debug("computeItemContext: Computed ppath for input page: " + p + " and got: " + ppath);
        }
        return ppath;
    }

    private List<AssetId> _pruneNodePathAndReverse(IList nodepath)
    {
        List<AssetId> list = new ArrayList<AssetId>();
        if(nodepath != null && nodepath.hasData())
        {
            if(LOG.isTraceEnabled())
            {
                LOG.trace("_pruneNodePathAndReverse: nodepath list returned " + nodepath.numRows() + " rows");
            }
            nodelist:
            for(IList row : new IterableIListWrapper(nodepath))
            {
                String nid = getStringValue(row, "nid");
                String type = getStringValue(row, "otype");
                String oid = getStringValue(row, "oid");

                if("Publication".equals(type))
                {
                    if(LOG.isTraceEnabled())
                    {
                        LOG.trace("_pruneNodePathAndReverse: looping over nodepath parents, hit Publication node.  " + "We must be at the top of the list.  Aborting loop. nid:" + nid + " pubid:" + oid);
                    }
                    break nodelist;
                }

                if(!"Page".equals(type))
                {
                    throw new IllegalStateException("Invalid node type found in SitePlanTree: " + type);
                }
                AssetId id = new AssetIdImpl(type, Long.valueOf(oid));
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("_pruneNodePathAndReverse: looping over nodepath parents.  Hit valid node: " + id + " nid: " + nid);
                }
                list.add(id);

            }

            // now reverse the list
            Collections.reverse(list);

            // trim extra entries
            if(list.size() >= lowestLevelToInclude)
            {
                for(int i = 0; i < lowestLevelToInclude; i++)
                {
                    AssetId id = list.remove(0); // shuffles all other versions back back
                    LOG.trace("Removing entry that is below the inclusion threshold: " + id);
                }
            }
            else
            {
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("_pruneNodePathAndReverse: Lowest level to include is set to a level that " + "is greater than the total number of elements in the breadcrumb.  " + "List size: " + list.size() + ", lowest level to include: " + lowestLevelToInclude);
                }
            }
        }
        return list;
    }

    private String _listToString(List<AssetId> list, final String localeName)
    {
        if(list.size() > 0)
        {
            StringBuilder sb = new StringBuilder();
            for(AssetId id : list)
            {
                if(id == null)
                {
                    throw new IllegalStateException("Found null value in ID list. List:" + list);
                }
                String cpath = computeAlias(id, localeName);
                if(cpath == null)
                {
                    if(LOG.isTraceEnabled())
                    {
                        LOG.trace("_listToString: Computed cpath for page " + id + " and failed to return a value.  Cannot compute ppath for " + list.get(list.size() - 1));
                    }
                    return null;
                }

                if(sb.length() > 0)
                {
                    sb.append("/");
                }
                sb.append(cpath);
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        else
        {
            LOG.trace("_listToString: Cannot compute ppath for input page because we pruned the list to exclude it.");
            return null;
        }
    }

    /**
     * Resolve the variable "p" for a given ppath.
     *
     * @param item_context item_context as computed in computePpath above.
     * @return ID of page asset
     */
    public long resolvePForItemContext(String item_context)
    {
        LOG.debug("resolvePForItemContext: Attempting to resolve P for ppath: " + item_context);
        CandidateInfo result = _resolvePForItemContext(item_context);
        if(LOG.isDebugEnabled())
        {
            LOG.debug("resolvePForItemContext: Found matching asset for ppath: " + item_context + ": " + result);
        }
        return result.getId().getId();
    }

    /**
     * Resolve the dimension for a given ppath.
     *
     * @param item_context item_context as computed in computePpath above.
     * @return dimension used to compute the input item_context. May be null if locale was not used to compute the
     *         item_context.
     */
    public Dimension resolveDimensionForItemContext(String item_context)
    {
        LOG.debug("resolveDimensionForItemContext: Attempting to resolve P for ppath: " + item_context);
        CandidateInfo result = _resolvePForItemContext(item_context);
        if(LOG.isDebugEnabled())
        {
            LOG.debug("resolveDimensionForItemContext: Found matching asset for ppath: " + item_context + ": " + result);
        }
        return result.getDim();
    }

    /**
     * Given an input asset type, alias, and p, resolve
     * cid.  Essentially this looks for c/alias pairs connected
     * to a specified p asset.
     *
     * @param c asset type of asset to resolve
     * @param alias of asset to resolve
     * @param p page to which the asset is associated
     * @return id of asset.  Never null.  If not resolvable, an exception is thrown (as this should never occur)
     */
    public long resolveCidFromAlias(String c, String alias, long p)
    {
        if(LOG.isDebugEnabled())
        {
            LOG.debug("resolveCidFromAlias: Attempting to resolve cid from c:cpath:p: " + c + ":" + alias + ":" + p);
        }
        // first, if c is a page, then we don't have to do any lookups at all.... cid IS p!
        if("Page".equals(c))
        {
            LOG.debug("resolveCidFromAlias: Input asset type was Page, so we resolved cid from the input value of p:" + p);
            return p;
        }

        // next try to load the asset by path. if we get a unique value, great!
        // if we don't get a unique value, load p, and try to see if p is associated
        // with one of the cpath candidates.
        final long result;
        List<CandidateInfo> candidates = translator.findCandidatesForAlias(c, alias);
        switch(candidates.size())
        {
            case 0:
            {
                throw new CSRuntimeException("Could not locate any assets in the database with a cpath matching: " + alias, ftErrors.badparams);
            }
            case 1:
            {
                long id = candidates.get(0).getId().getId();
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("resolveCidFromAlias: Resolved cid to " + id + " from c:cpath without using p");
                }
                result = id;
                break;
            }
            default:
            {
                // Too bad.  Not a unique name.  Try to find an asset in the candidate
                // list that is associated with p.
                List<AssetId> pageKids = _findChildrenOfPage(p);

                List<AssetId> matches = new ArrayList<AssetId>();
                for(CandidateInfo candidate : candidates)
                {
                    if(pageKids.contains(candidate.getId()))
                    {
                        matches.add(candidate.getId());
                    }
                }

                result = _processMatchesForAliasP(alias, p, candidates, matches);
            }
        }
        if(LOG.isDebugEnabled())
        {
            LOG.debug("resolveCidFromAlias: Successfully resolved cid from c:cpath:p: " + c + ":" + alias + ":" + p + " and found: " + result);
        }
        return result;
    }

    /**
     * Resolve the matching candidate for the specified input ppath variable.
     *
     * @param item_context input ppath
     * @return the matching candidate
     * @throws CSRuntimeException in case no match can be found.
     */
    private CandidateInfo _resolvePForItemContext(String item_context)
    {
        LOG.trace("_resolvePForItemContext: Attempting to resolve P for ppath: " + item_context);
        if(item_context == null)
        {
            throw new IllegalArgumentException("Null ppath not allowed");
        }

        String[] breadcrumb = item_context.split("/");

        List<CandidateInfo> rightmostCandidates = translator.findCandidatesForAlias("Page", breadcrumb[breadcrumb.length - 1]);

        for(CandidateInfo rightmostCandidate : rightmostCandidates)
        {
            String candidatePpath = computeItemContext(rightmostCandidate.getId().getId(), rightmostCandidate.getDim() == null ? null : rightmostCandidate.getDim().getName());
            if(item_context.equals(candidatePpath))
            {
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("_resolvePForItemContext: Found matching asset for ppath: " + item_context + ": " + rightmostCandidate);
                }
                return rightmostCandidate;
            }
        }

        throw new CSRuntimeException("No page found that matches the ppath specified: " + item_context, ftErrors.pagenotfound);
    }

    private long _processMatchesForAliasP(String alias, long p, List<CandidateInfo> candidates, List<AssetId> matches)
    {
        switch(matches.size())
        {
            case 0:
            {
                String s = "No assets matching cpath:" + alias + " were found on Page:" + p + " but " + candidates.size() + " assets were found matching cpath.  Returning the first one: " + matches.get(0);
                LOG.warn(s);
                return candidates.get(0).getId().getId();

            }
            case 1:
            {
                if(LOG.isDebugEnabled())
                {
                    LOG.debug("_processMatchesForAliasP: Found multiple assets matching cpath:" + alias + " but found a unique one that was placed on Page:" + p + ": " + matches.get(0));
                }
                return matches.get(0).getId();
            }
            default:
            {
                String s = "Found multiple assets matching cpath:" + alias + " (" + matches + ") and found that more than one of them was associated with Page:" + p + ".  Returning the first match: " + matches.get(0);
                LOG.warn(s);
                return matches.get(0).getId();
            }
        }
    }

    private List<AssetId> _findChildrenOfPage(long p)
    {
        Children assetChildren = new Children();
        assetChildren.setAssetId(Long.toString(p));
        assetChildren.setType("Page");
        assetChildren.setList("__kids");
        assetChildren.execute(ics);
        if(ics.GetErrno() < 0 && ics.GetErrno() != ftErrors.norows)
        {
            throw new CSRuntimeException("Failure getting children for page asset", ics.GetErrno());
        }

        IList kids = ics.GetList("__kids");
        ics.RegisterList("__kids", null);
        List<AssetId> kidIdList;
        if(kids == null || !kids.hasData())
        {
            kidIdList = Collections.emptyList();
        }
        else
        {
            kidIdList = new ArrayList<AssetId>();
            for(IList kidRow : new IterableIListWrapper(kids))
            {
                long id = getLongValue(kidRow, "oid");
                String type = getStringValue(kidRow, "otype");
                kidIdList.add(new AssetIdImpl(type, id));
            }
        }
        return kidIdList;
    }
}
