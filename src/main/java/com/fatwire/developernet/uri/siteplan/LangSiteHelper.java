/*
 * Copyright (c) 2009 FatWire Corporation. All Rights Reserved.
 * Title, ownership rights, and intellectual property rights in and
 * to this software remain with FatWire Corporation. This  software
 * is protected by international copyright laws and treaties, and
 * may be protected by other law.  Violation of copyright laws may
 * result in civil liability and criminal penalties.
 */
package com.fatwire.developernet.uri.siteplan;

import COM.FutureTense.Interfaces.ICS;
import com.fatwire.developernet.uri.itemcontext.aliasing.CandidateInfo;
import com.fatwire.mda.Dimension;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Derives language from the page asset.
 * Site is specified as an input and is NOT automatically derived from the site plan tree (though it would be easy
 * to do).  The reason it is not auto-generated is that we want the ability to specify a "site" that may just be a
 * set of subfolders inside the SitePlan tree, rather than a whole new UI site.  For example, public/private trees
 * within a given site that need to be represented in the URL as 2 sites.
 *
 * Todo: Better configuration for language-alias and site-alias should be done, but this simple class is a good example
 * to get users started writing their own helpers.  As you can see, language-alias and site-alias have to be done by the
 * caller instead of by this utility, which would be the logical place fo rit.
 *
 * Note: September 4, 2009 by Tony Field 17:11 - This class has not been tested yet.
 *
 * @author Tony Field
 * @see Helper
 * @since Sep 4, 2009
 */
public final class LangSiteHelper
{
    private static final Log LOG = LogFactory.getLog("com.fatwire.developernet.uri.siteplan.helper");

    private final ICS ics;
    private final Helper helper;

    public LangSiteHelper(ICS ics)
    {
        this.ics = ics;
        helper = new Helper(ics);
    }

    /**
     * Reads the following params from the assembler:
     * - item-context
     * - item-type
     * - item-alias
     * and sets the following params into ICS
     * - language-alias
     * - site-alias
     * - p
     * - c
     * - cid
     *
     * Call this from your wrapper to pre-populate all of the relevant variables.
     *
     * @param ics ICS context
     * @locale id of the locale
     * @return dimension of the resolved page
     * @see Helper#resolveItemContextAliasesAndPopulateIcs
     */
    public static Dimension resolveItemContextAliasesAndPopulateIcs(ICS ics, String locale)
    {
        LangSiteHelper helper = new LangSiteHelper(ics);

        String lang_site_item_context = ics.GetVar("item-context");
        ItemContextInfo ici = getItemContextInfo(ics, lang_site_item_context);
        String item_context = ici.getRegularItemContext();

        String language_alias = ici.getLanguageAlias();
        ics.SetVar("language-alias", language_alias);
        if(LOG.isDebugEnabled())
        {
            LOG.debug("Populated ICS with resolved language-alias:" + language_alias + " from item-context:" + lang_site_item_context);
        }

        String site_alias = ici.getSiteAlias();
        ics.SetVar("site-alias", site_alias);
        if(LOG.isDebugEnabled())
        {
            LOG.debug("Populated ICS with resolved site-alias:" + site_alias + " from item-context:" + lang_site_item_context);
        }


        Dimension dimension = null;
        long longP = -1L;
        if(item_context != null)
        {
            if(ics.GetVar("p") != null)
            {
                LOG.warn("Unexpected value of p found in conjunction with item-context:" + ics.GetVar("p") + ", " + item_context + ".  Ignoring item-context.");
            }
            else
            {
                // Minor cheat here in the static helper function:
                // instead of just calling the same function twice,
                // once to get 'p' and another time to get 'Dimension',
                // just use the internal worker function that does
                // both.  End users will not notice unless they parse
                // logs and count queries.
                CandidateInfo result = helper.resolveItemContext(item_context, locale);
                longP = result.getId().getId();
                dimension = result.getDim();
                ics.SetVar("p", Long.toString(longP));
                if(LOG.isDebugEnabled())
                {
                    LOG.debug("Populated ICS with resolved p:" + longP + " from item-context:" + item_context);
                    LOG.debug("About to return dimension: " + dimension + " from item-context:" + item_context);
                }
            }
        }

        String c = ics.GetVar("item-type");
        String item_alias = ics.GetVar("item-alias");
        if(item_alias != null)
        {
            if(ics.GetVar("cid") != null)
            {
                LOG.warn("Unexpected value of cid found in conjunction with item-alias:" + ics.GetVar("cid") + ", " + item_alias + ".  Ignoring item-alias.");
            }
            else
            {
                long longCid = helper.resolveCidFromAlias(c, item_alias, longP, locale);
                ics.SetVar("cid", Long.toString(longCid));
                if(LOG.isDebugEnabled())
                {
                    LOG.debug("Populated ICS with resolved cid:" + longCid + " from item-alias:" + item_alias);
                }
            }
        }

        return dimension;
    }

    CandidateInfo resolveItemContext(String regular_item_context, String locale)
    {
        return helper.resolveItemContext(regular_item_context, locale);
    }

    public String computeAlias(String c, long cid, String localeName)
    {
        return helper.computeAlias(c, cid, localeName);
    }

    public String computeItemContext(String site_alias, long p, final String localeName)
    {
        String language_alias = localeName.substring(0, localeName.indexOf("_"));
        return language_alias + "/" + site_alias +"/" + helper.computeItemContext(p, localeName);
    }

    public String resolveSiteForItemContext(String item_context)
    {
        return getItemContextInfo(ics, item_context).getSiteAlias();
    }

    public String resolveLanguageForItemContext(String item_context)
    {
        return getItemContextInfo(ics, item_context).getLanguageAlias();
    }

    public long resolvePForItemContext(String item_context, String locale)
    {
        String regularItemContext = getItemContextInfo(ics, item_context).getRegularItemContext();
        return helper.resolvePForItemContext(regularItemContext, locale);
    }

    public Dimension resolveDimensionForItemContext(String item_context, String locale)
    {
        String regularItemContext = getItemContextInfo(ics, item_context).getRegularItemContext();
        return helper.resolveDimensionForItemContext(regularItemContext, locale);
    }

    public long resolveCidFromAlias(String c, String alias, long p, String locale)
    {
        return helper.resolveCidFromAlias(c, alias, p, locale);
    }

    private static ItemContextInfo getItemContextInfo(ICS ics, String lang_site_itemcontext)
    {
        // todo: Note from Tony Field September 4, 2009: optimization
        // if the string parsing in the constructor is too intense,
        // this object can be cached on the ICS object pool.
        // I don't really think it will be necessary though.
        return new ItemContextInfo(lang_site_itemcontext);
    }

    private static class ItemContextInfo
    {
        private final String siteAlias;
        private final String languageAlias;
        private final String regularItemContext;

        private ItemContextInfo(String lang_site_itemcontext)
        {
            int firstslash = lang_site_itemcontext.indexOf('/');
            int secondslash = lang_site_itemcontext.indexOf('/', firstslash + 1);
            languageAlias = lang_site_itemcontext.substring(0, firstslash);
            siteAlias = lang_site_itemcontext.substring(firstslash + 1, secondslash);
            regularItemContext = lang_site_itemcontext.substring(secondslash + 1);
        }

        public String getSiteAlias()
        {
            return siteAlias;
        }

        public String getLanguageAlias()
        {
            return languageAlias;
        }

        public String getRegularItemContext()
        {
            return regularItemContext;
        }
    }
}
