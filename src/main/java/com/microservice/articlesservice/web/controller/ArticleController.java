package com.microservice.articlesservice.web.controller;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.microservice.articlesservice.dao.ArticleDao;
import com.microservice.articlesservice.model.Article;
import com.microservice.articlesservice.web.exceptions.ArticleIntrouvableException;
import com.microservice.articlesservice.web.exceptions.ArticlePrixVente;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Api(description = "API pour les opérations CRUD sur les articles.")
public class ArticleController
{
    @Autowired
    private ArticleDao articleDao;

    //**GET**
    //Liste tout les articles
    //----> http://localhost:9090/Articles
    @ApiOperation(value = "Récupère tous les articles en stock!")
    @RequestMapping(value="/Articles", method= RequestMethod.GET)
    public MappingJacksonValue listeArticles()
    {
      List<Article> articles = articleDao.findAll();

      SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("prixAchat");
      FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);
      MappingJacksonValue articlesFiltres = new MappingJacksonValue( articles );
      articlesFiltres .setFilters( listDeNosFiltres );

      return articlesFiltres ;
    }


    //**GET**
    //Récupérer un article par son Id
    // ----> http://localhost:9090/Articles/42
    @ApiOperation(value = "Récupère un article grâce à son ID à condition que celui-ci soit en stock!")
    @GetMapping(value="/Articles/{id}")
    public Article afficherUnArticle(@PathVariable int id)
    {
        Article article = articleDao.findById(id);
        if(article==null)
            throw new ArticleIntrouvableException("L'article avec l'id : " + id + "est INTROUVABLE");

        return article;
    }



    //**POST**
    //ajouter un article
    //----> http://localhost:9090/Articles
    @ApiOperation(value = "Ajoute un article au stock")
    @PostMapping(value = "/Articles")
    public ResponseEntity< Void > ajouterArticle (@RequestBody Article article)
    {
        Article articleAdded = articleDao.save(article);
        if(articleAdded.getPrix() == 0)
            throw  new ArticlePrixVente("Attention l'article avec l'id" + articleAdded.getId() + "à un prix de vente égal à 0");
        if ( articleAdded == null )
            return ResponseEntity . noContent ().build();
        URI location = ServletUriComponentsBuilder
                . fromCurrentRequest ()
                .path( "/{id}" )
                .buildAndExpand( articleAdded.getId())
                .toUri();
        return ResponseEntity . created ( location ).build();
    }

    //**DELETE**
    //un article par l'id
    //----> http://localhost:9090/Articles/1
    @ApiOperation(value = "Supprime un article par l'id")
    @DeleteMapping(value = "/Articles/{id}")
    public void supprimerArticle(@PathVariable int id)
    {
        articleDao.deleteById(id);
    }

    //**PUT**
    //MAJ d'un article
    //----> http://localhost:9090/Articles
    @ApiOperation(value = "Update un article mis à jour")
    @PutMapping (value = "/Articles")
    public void updateArticle(@RequestBody Article article)
    {
        articleDao.save(article);
    }

    //**GET**
    // Calcul de la marge
    //----> http://localhost:9090/AdminArticles
    @ApiOperation(value = "Calcul la marge entre chaque article")
    @GetMapping(value = "/AdminArticles")
    public List<String> calculerMargeArticle()
    {
        List<Article> articles = articleDao.findAll();
        List<String> array = new ArrayList<>();
        for(Article article:articles)
        {
            int marge = article.getPrix() - article.getPrixAchat();
            String resultat = "Pour l'article : " + article.getNom() + " votre marge est de : "+ marge + " euros.";
            array.add(resultat);
        }
        return array;
    }


    //**GET**
    // Retourne la liste de tous les articles par nom croissant
    // ----> http://localhost:9090/Articles/alpha
    @ApiOperation(value = "Retourne la liste de tous les articles triés par nom croisant")
    @GetMapping(value = "/Articles/alpha")
    public MappingJacksonValue trierArticlesParOrdreAlphabetique()
    {
        List<Article> articles = articleDao.findAllByOrderByNom();

        SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("prixAchat");
        FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);
        MappingJacksonValue articlesFiltres = new MappingJacksonValue( articles );
        articlesFiltres.setFilters( listDeNosFiltres );
        return articlesFiltres ;
    }


    // TEST PrixLimit

    @GetMapping(value = "/test/articles/{prixLimit}")
    public List<Article> testeDeRequetes(@PathVariable int prixLimit)
    {
        return articleDao.findByPrixGreaterThan(prixLimit);
    }
    // TEST FindByNom

    @GetMapping (value = "/test/articles/like/{recherche}" )
    public List<Article> testeDeRequetes ( @PathVariable String recherche)
    {
        return articleDao.findByNomLike( "%" +recherche+ "%" );
    }


}
