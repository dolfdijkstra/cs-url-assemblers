/*
 * Copyright (c) 2009 FatWire Corporation. All Rights Reserved.
 * Title, ownership rights, and intellectual property rights in and
 * to this software remain with FatWire Corporation. This  software
 * is protected by international copyright laws and treaties, and
 * may be protected by other law.  Violation of copyright laws may
 * result in civil liability and criminal penalties.
 */
package com.fatwire.developernet.uri.itemcontext.aliasing;

import COM.FutureTense.Interfaces.ICS;
import COM.FutureTense.Interfaces.Utilities;
import COM.FutureTense.Util.ftErrors;
import com.fatwire.assetapi.common.AssetAccessException;
import com.fatwire.assetapi.data.*;
import com.fatwire.developernet.CSRuntimeException;
import com.fatwire.developernet.facade.mda.DimensionUtils;
import static com.fatwire.developernet.facade.runtag.example.asset.AssetRelationTreeUtils.getAssetRelationTreeParents;
import com.fatwire.mda.Dimension;
import com.fatwire.mda.DimensionableAssetManager;
import com.fatwire.system.SessionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * This translation strategy uses the path field for all asset types, just
 * as in the PathFieldTranslationStrategy, but for Page assets, it looks up
 * an article by a named association, translates it into the desired locale,
 * and uses that asset instead.
 */
public class MultilingualPageMetadataArticleAliasingStrategy implements AssetAliasingStrategy
{
    private static Log LOG = LogFactory.getLog(MultilingualPageMetadataArticleAliasingStrategy.class.getName());

    /**
     * Name of configuration file.  If configuration properties are not in this
     * file, Java System properties will be checked.  the file must be in the classpath
     * or in the inipath.
     */
    public static final String CONFIGURATION_FILE_NAME = "ServletRequest.properties";

    /**
     * Property name defining the name of the association to be used to look up an associated article from
     * the page asset
     */
    public static final String PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME = "com.fatwire.developernet.uri.itemcontext.aliasing.MultilingualPageMetadataArticleAliasingStrategy.association-name";
    /**
     * Default value of the {@link #PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME} property.
     */
    public static final String PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME_DEFAULT = "MetadataArticle";
    /**
     * Property name defining the asset type of the article that is associated with the page asset.
     */
    public static final String PROPERTY_PAGE_ARTICLE_ASSET_TYPE = "com.fatwire.developernet.uri.itemcontext.aliasing.MultilingualPageMetadataArticleAliasingStrategy.article-type";
    /**
     * Default value of the {@link #PROPERTY_PAGE_ARTICLE_ASSET_TYPE} property
     */
    public static final String PROPERTY_PAGE_ARTICLE_ASSET_TYPE_DEFAULT = "Article";

    private final ICS ics;
    private final String associationNameForPage;
    private final PathAliasingStrategy pathTranslationStrategy;
    private final String articleAssetType;
    private final AssetDataManager adm;
    private final DimensionableAssetManager dam;

    public MultilingualPageMetadataArticleAliasingStrategy(ICS ics)
    {
        this.ics = ics;
        pathTranslationStrategy = new PathAliasingStrategy(ics);
        associationNameForPage = _getProperty(PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME, PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME_DEFAULT);
        articleAssetType = _getProperty(PROPERTY_PAGE_ARTICLE_ASSET_TYPE, PROPERTY_PAGE_ARTICLE_ASSET_TYPE_DEFAULT);
        adm = (AssetDataManager)SessionFactory.getSession(ics).getManager(AssetDataManager.class.getName());
        dam = (DimensionableAssetManager)SessionFactory.getSession(ics).getManager(DimensionableAssetManager.class.getName());
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


    public String computeAlias(AssetId id, final String localeName)
    {
        if(LOG.isTraceEnabled())
        {
            LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.computeAlias: computing cpath from id-locale: " + id + "-" + localeName);
        }
        final String result;
        if("Page".equals(id.getType()))
        {
            try
            {
                AssetData pageData = adm.readAttributes(id, Arrays.asList("name")); // todo: don't even need name
                List<AssetId> assoc = pageData.getAssociatedAssets(associationNameForPage);
                switch(assoc.size())
                {
                    case 0:
                        if(LOG.isTraceEnabled())
                        {
                            LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.computeAlias: No association named " + associationNameForPage + " found on page " + id);
                        }
                        result = null;
                        break;
                    case 1:
                        AssetId article = assoc.get(0);
                        final AssetId translated;
                        if(LOG.isTraceEnabled())
                        {
                            LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.computeAlias: Found one asset (" + article + ") associated to page " + id + " using association named " + associationNameForPage);
                        }
                        Dimension dim = DimensionUtils.getLocaleAsDimension(ics, article);
                        if(dim.getName().equals(localeName))
                        {
                            translated = article;
                        }
                        else
                        {
                            translated = dam.getRelative(assoc.get(0), localeName);
                        }
                        if(translated == null)
                        {
                            if(LOG.isTraceEnabled())
                            {
                                LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.computeAlias: Attempted to translate article " + assoc.get(0) + " into locale " + localeName + " but found no result.");
                            }
                            result = null;
                            break;
                        }
                        else
                        {
                            if(LOG.isTraceEnabled())
                            {
                                LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.computeAlias: Translated article from " + assoc.get(0) + " to " + translated);
                            }
                            result = pathTranslationStrategy.computeAlias(translated, null); // do not translate anymore
                            break;
                        }
                    default:
                        LOG.warn("More than one association found for association named " + associationNameForPage + " for page " + id + " when only one was expected.  List of results: " + assoc);
                        result = null;
                        break;
                }

            }
            catch(AssetAccessException e)
            {
                throw new CSRuntimeException("Failure accessing data for asset " + id, ftErrors.unknownerror, e);
            }
        }
        else
        {
            result = pathTranslationStrategy.computeAlias(id, null); // only pages support translation
        }
        if(LOG.isTraceEnabled())
        {
            LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.computeAlias: computed cpath from id-locale: " + id + "-" + localeName + " and got: " + result);
        }
        return result;
    }

    public List<CandidateInfo> findCandidatesForAlias(String c, String cpath)
    {
        if(LOG.isTraceEnabled())
        {
            LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.findCandidatesForAlias: looking for candidates for c:cpath:" + c + ":" + cpath);
        }
        if("Page".equals(c))
        {

            List<CandidateInfo> matchingArticles = pathTranslationStrategy.findCandidatesForAlias(articleAssetType, cpath);

            if(matchingArticles.size() == 0)
            {
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.findCandidatesForAlias: Could not find any assets of type: " + articleAssetType + " with cpath: " + cpath + ", so it is not possible to find the page whose cpath is " + cpath);
                }
            }

            List<CandidateInfo> candidatePages = new ArrayList<CandidateInfo>();
            for(CandidateInfo article : matchingArticles)
            {
                // look up article in ART with specified assoc name
                List<AssetId> foundPages = getAssetRelationTreeParents(ics, LOG, article.getId(), "Page", associationNameForPage);
                if(foundPages.size() > 0)
                {
                    for(AssetId foundPage : foundPages)
                    {
                        // Note we need to know what locale generated this path
                        candidatePages.add(new CandidateInfo(foundPage, article.getDim()));
                    }
                }
                else
                {
                    if(LOG.isTraceEnabled())
                    {
                        LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.findCandidatesForAlias: Did not find the " + article + " associated to a page using the association " + associationNameForPage + " so we have to look it up in other locales in case the locale found on the path is not the locale associated with the Page.");
                    }
                    for(AssetId translationOfMatch : dam.getRelatives(article.getId(), null))
                    {
                        foundPages = getAssetRelationTreeParents(ics, LOG, translationOfMatch, "Page", associationNameForPage);
                        if(foundPages.size() > 0)
                        {
                            // todo: remove? Dimension translationDim = getLocaleAsDimension(ics, translationOfMatch);
                            for(AssetId foundPage : foundPages)
                            {
                                candidatePages.add(new CandidateInfo(foundPage, article.getDim()));
                            }
                        }
                    }
                }
            }
            if(LOG.isTraceEnabled())
            {
                LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.findCandidatesForAlias: found candidates for c:cpath:" + c + ":" + cpath + " and got: " + candidatePages);
            }
            return candidatePages;
        }
        else
        {
            List<CandidateInfo> result = pathTranslationStrategy.findCandidatesForAlias(c, cpath);
            if(LOG.isTraceEnabled())
            {
                LOG.trace("MultilingualPageMetadataArticleAliasingStrategy.findCandidatesForAlias: found candidates for c:cpath:" + c + ":" + cpath + " and got: " + result);
            }
            return result;
        }
    }
}
