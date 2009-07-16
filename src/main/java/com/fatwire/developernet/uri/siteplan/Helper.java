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
 * Helper class for dealing with the ItemContext assembler that is based on the Site Plan Tree.  Specifically,
 * When the context is defined as a concatenation of sequential page aliases in the Site plan tree, this assembler
 * is appropriate.  In other words, <code>http://localhost:8080/cs/Satellite/[level1page]/[level2page]/[level3page]<code>
 * urls and related URLs are supported.
 *
 * This helper class relies on a valid {@link AssetAliasingStrategy} being configured.
 * 
 * // todo: greatly enhance documentation
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
     */
    public static final String PROPERTY_MAX_DEPTH_PROP_NAME = "com.fatwire.developernet.uri.siteplan.helper.lowest-level-to-include";

    /**
     * Property specifying the asset aliasing strategy to use.  Specify the classname for the strategy.
     */
    public static final String PROPERTY_ALIASING_STRATEGY = "com.fatwire.developernet.uri.siteplan.helper.aliasing-strategy-class";

    /**
     * Default value for the {@link #PROPERTY_ALIASING_STRATEGY} property.
     */
    public static final String PROPERTY_ALIASING_STRATEGY_DEFAULT = PathAliasingStrategy.class.getName();

    /**
     * The default value of the {@link #PROPERTY_MAX_DEPTH_PROP_NAME} if not otherwise specified.
     * The default value is 1, which supports a single level of navigation placeholder pages
     * in the SitePlanTree - the most common configuration.
     */
    public static final String PROPERTY_MAX_DEPTH_PROP_NAME_DEFAULT = "1";

    private final int lowestLevelToInclude;
    private final ICS ics;
    private final AssetAliasingStrategy translator;

    /**
     * Helper class for looking up cpath and ppath in URLs.
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
     * Given an input c/cid pair, compute (/look up) the cpath for that asset.
     * <p/>
     * This function is not to be called from within the assembler, but within
     * Content Server.
     * <p/>
     * The return value may be null, indicating that no cpath exists.
     *
     * @param c asset type
     * @param cid asset id
     * @param localeName name of the locale for which the path should be computed
     * @return path for the asset, or else null
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
     * for it.  If any page asset does not have a path specified, no path is returned.
     * <p/>
     * Note that the path for each individual element is computed using the {@link #computeAlias}
     * method defined above.  This allows for the path attribute to be provided in various ways,
     * such as by looking up the "path" field or by looking up the "path" field of an associated asset.
     *
     * @param p page id
     * the publication itself, Level 1 is typically "Placed" or "Unplaced".
     * @param localeName name of locale
     * @return path or null
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
                        LOG.trace("_pruneNodePathAndReverse: looping over nodepath parents, hit Publication node.  " +
                                  "We must be at the top of the list.  Aborting loop. nid:" + nid + " pubid:" + oid);
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
                    LOG.trace("_pruneNodePathAndReverse: Lowest level to include is set to a level that " +
                              "is greater than the total number of elements in the breadcrumb.  " +
                              "List size: " + list.size() + ", lowest level to include: " + lowestLevelToInclude);
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
     * @param item_context ppath as computed in computePpath above.
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
     * @param item_context ppath as computed in computePpath above.
     * @return dimension used to compute the input ppath. May be null if locale was not used to compute the ppath.
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
