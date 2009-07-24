/*
 * Copyright (c) 2009 FieldCo, Inc. All Rights Reserved.
 */
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
import static com.fatwire.developernet.facade.runtag.example.asset.AssetRelationTreeUtils.getAssetRelationTreeParents;
import com.fatwire.system.SessionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * This translation strategy uses the path field for all asset types, just
 * as in the {@link PathAliasingStrategy}, but for Page assets, it looks up
 * an article by a named association, and uses that asset instead.
 * <p/>
 * <p><strong>Configuration</strong></p>
 * <p>This aliasing strategy offers two configuration parameters, with reasonable default values.
 * It needs to know which named association to use to retrieve the asset containing the alias information, and
 * it needs to know what type of asset that is expected to be:</p>
 * <p>{@link #PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME} is the property name corresponding to the association name,
 * and {@link #PROPERTY_PAGE_ARTICLE_ASSET_TYPE} contains the asset type that should be retrieved from the
 * association.</p>
 * <p>Properties are configured in the {@link #CONFIGURATION_FILE_NAME} file.</p>
 *
 * @author Tony Field
 * @since Jun 1, 2009
 */
public class PageMetadataArticleAliasingStrategy implements AssetAliasingStrategy
{
    /**
     * Name of configuration file.  If configuration properties are not in this
     * file, Java System properties will be checked.  the file must be in the classpath
     * or in the inipath.
     */
    public static final String CONFIGURATION_FILE_NAME = "ServletRequest.properties";

    /**
     * Property name defining the name of the association to be used to look up an associated article from
     * the page asset
     *
     * @see #PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME_DEFAULT
     */
    public static final String PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME = "uri.assembler.SitePlanPpathCCpathAssembler.page-metadata-article-translator.association-name";
    /**
     * Default value of the {@link #PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME} property.
     */
    public static final String PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME_DEFAULT = "MetadataArticle";
    /**
     * Property name defining the asset type of the article that is associated with the page asset.
     *
     * @see #PROPERTY_PAGE_ARTICLE_ASSET_TYPE_DEFAULT
     */
    public static final String PROPERTY_PAGE_ARTICLE_ASSET_TYPE = "uri.assembler.SitePlanPpathCCpathAssembler.page-metadata-article-translator.article-type";
    /**
     * Default value of the {@link #PROPERTY_PAGE_ARTICLE_ASSET_TYPE} property
     */
    public static final String PROPERTY_PAGE_ARTICLE_ASSET_TYPE_DEFAULT = "Article";

    private static Log LOG = LogFactory.getLog(PageMetadataArticleAliasingStrategy.class.getName());
    private final ICS ics;
    private final String associationNameForPage;
    private final PathAliasingStrategy pathTranslationStrategy;
    private final String articleAssetType;
    private final AssetDataManager adm;

    public PageMetadataArticleAliasingStrategy(ICS ics)
    {
        this.ics = ics;
        pathTranslationStrategy = new PathAliasingStrategy(ics);
        associationNameForPage = _getProperty(PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME, PROPERTY_PAGE_ARTICLE_ASSOCIATION_NAME_DEFAULT);
        articleAssetType = _getProperty(PROPERTY_PAGE_ARTICLE_ASSET_TYPE, PROPERTY_PAGE_ARTICLE_ASSET_TYPE_DEFAULT);
        adm = (AssetDataManager)SessionFactory.getSession(ics).getManager(AssetDataManager.class.getName());
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


    public String computeAlias(AssetId id, String localeName)
    {
        if(LOG.isTraceEnabled())
        {
            LOG.trace("PageMetadataArticleAliasingStrategy.computeAlias: computing cpath from id-locale: " + id + "-" + localeName);
        }
        final String result;
        if("Page".equals(id.getType()))
        {
            try
            {
                AssetData pageData = adm.readAttributes(id, Arrays.asList("name"));
                List<AssetId> assoc = pageData.getAssociatedAssets(associationNameForPage);
                switch(assoc.size())
                {
                    case 0:
                        if(LOG.isTraceEnabled())
                        {
                            LOG.trace("PageMetadataArticleAliasingStrategy.computeAlias: No association named " + associationNameForPage + " found on page " + id);
                        }
                        result = null;
                        break;
                    case 1:
                        if(LOG.isTraceEnabled())
                        {
                            LOG.trace("PageMetadataArticleAliasingStrategy.computeAlias: Found one asset (" + assoc.get(0) + ") associated to page " + id + " using association named " + associationNameForPage);
                        }
                        result = pathTranslationStrategy.computeAlias(assoc.get(0), null); // translations not required
                        break;
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
            result = pathTranslationStrategy.computeAlias(id, null); // translations not supported
        }
        if(LOG.isTraceEnabled())
        {
            LOG.trace("PageMetadataArticleAliasingStrategy.computeAlias: computed cpath from id-locale: " + id + "-" + localeName + " and got: " + result);
        }
        return result;
    }

    public List<CandidateInfo> findCandidatesForAlias(String c, String cpath)
    {
        if(LOG.isTraceEnabled())
        {
            LOG.trace("PageMetadataArticleAliasingStrategy.findCandidatesForAlias: looking for candidates for c:cpath:" + c + ":" + cpath);
        }
        if("Page".equals(c))
        {

            List<CandidateInfo> matchingArticles = pathTranslationStrategy.findCandidatesForAlias(articleAssetType, cpath);

            if(matchingArticles.size() == 0)
            {
                if(LOG.isTraceEnabled())
                {
                    LOG.trace("PageMetadataArticleAliasingStrategy.findCandidatesForAlias: Could not find any assets of type: " + articleAssetType + " with cpath: " + cpath + ", so it is not possible to find the page whose cpath is " + cpath);
                }
            }

            List<CandidateInfo> candidatePages = new ArrayList<CandidateInfo>();
            for(CandidateInfo article : matchingArticles)
            {
                // look up article in ART with specified assoc name
                List<AssetId> pages = getAssetRelationTreeParents(ics, LOG, article.getId(), "Page", associationNameForPage);
                for(AssetId page : pages)
                {
                    candidatePages.add(new CandidateInfo(page, article.getDim())); // we care about the dim of the article that produced the page's path
                }
            }
            if(LOG.isTraceEnabled())
            {
                LOG.trace("PageMetadataArticleAliasingStrategy.findCandidatesForAlias: found candidates for c:cpath:" + c + ":" + cpath + " and got: " + candidatePages);
            }
            return candidatePages;
        }
        else
        {
            List<CandidateInfo> result = pathTranslationStrategy.findCandidatesForAlias(c, cpath);
            if(LOG.isTraceEnabled())
            {
                LOG.trace("PageMetadataArticleAliasingStrategy.findCandidatesForAlias: found candidates for c:cpath:" + c + ":" + cpath + " and got: " + result);
            }
            return result;
        }
    }
}
