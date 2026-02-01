package com.example.s2.service;

import com.example.s2.domain.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Elasticsearch search operations.
 * Manages indexing and searching of Customer entities.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final String INDEX_NAME = "customers";
    private static final IndexCoordinates INDEX_COORDINATES = IndexCoordinates.of(INDEX_NAME);

    private final ElasticsearchTemplate elasticsearchTemplate;

    public SearchService(ElasticsearchTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        ensureIndexExists();
    }

    /**
     * Indexes a customer in Elasticsearch.
     *
     * @param customer the customer to index
     */
    public void index(Customer customer) {
        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(customer.getId().toString())
                .withObject(customerToDocument(customer))
                .build();

        elasticsearchTemplate.index(indexQuery, INDEX_COORDINATES);
        elasticsearchTemplate.indexOps(INDEX_COORDINATES).refresh();
        log.debug("Indexed customer: {}", customer.getId());
    }

    /**
     * Removes a customer from the search index.
     *
     * @param id the customer ID
     */
    public void delete(UUID id) {
        String deletedId = elasticsearchTemplate.delete(id.toString(), INDEX_COORDINATES);
        if (deletedId != null) {
            elasticsearchTemplate.indexOps(INDEX_COORDINATES).refresh();
            log.debug("Deleted customer from index: {}", id);
        }
    }

    /**
     * Searches customers by name.
     *
     * @param name the name to search for
     * @return list of matching customers
     */
    public List<CustomerSearchResult> searchByName(String name) {
        Criteria criteria = new Criteria("name").contains(name);
        CriteriaQuery query = new CriteriaQuery(criteria);

        SearchHits<CustomerDocument> hits = elasticsearchTemplate.search(query, CustomerDocument.class, INDEX_COORDINATES);
        return hits.getSearchHits().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * Searches customers by email.
     *
     * @param email the email to search for
     * @return list of matching customers
     */
    public List<CustomerSearchResult> searchByEmail(String email) {
        Criteria criteria = new Criteria("email").is(email);
        CriteriaQuery query = new CriteriaQuery(criteria);

        SearchHits<CustomerDocument> hits = elasticsearchTemplate.search(query, CustomerDocument.class, INDEX_COORDINATES);
        return hits.getSearchHits().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * Full-text search across name, email, and address.
     *
     * @param searchTerm the term to search for
     * @return list of matching customers
     */
    public List<CustomerSearchResult> fullTextSearch(String searchTerm) {
        Criteria criteria = new Criteria("name").contains(searchTerm)
                .or(new Criteria("email").contains(searchTerm))
                .or(new Criteria("address").contains(searchTerm));
        CriteriaQuery query = new CriteriaQuery(criteria);

        SearchHits<CustomerDocument> hits = elasticsearchTemplate.search(query, CustomerDocument.class, INDEX_COORDINATES);
        return hits.getSearchHits().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * Gets a customer document by ID from the search index.
     *
     * @param id the customer ID
     * @return the search result if found
     */
    public Optional<CustomerSearchResult> findById(UUID id) {
        CustomerDocument doc = elasticsearchTemplate.get(id.toString(), CustomerDocument.class, INDEX_COORDINATES);
        if (doc != null) {
            return Optional.of(new CustomerSearchResult(
                    UUID.fromString(doc.id),
                    doc.name,
                    doc.email,
                    doc.address,
                    1.0f
            ));
        }
        return Optional.empty();
    }

    /**
     * Checks if a customer is indexed.
     *
     * @param id the customer ID
     * @return true if indexed
     */
    public boolean isIndexed(UUID id) {
        return elasticsearchTemplate.exists(id.toString(), INDEX_COORDINATES);
    }

    /**
     * Gets the total number of indexed customers.
     *
     * @return the document count
     */
    public long count() {
        return elasticsearchTemplate.count(new CriteriaQuery(new Criteria()), INDEX_COORDINATES);
    }

    /**
     * Clears all documents from the index.
     * Use with caution - mainly for testing purposes.
     */
    public void clearAll() {
        if (elasticsearchTemplate.indexOps(INDEX_COORDINATES).exists()) {
            elasticsearchTemplate.indexOps(INDEX_COORDINATES).delete();
            ensureIndexExists();
            log.info("Cleared all documents from customers index");
        }
    }

    private void ensureIndexExists() {
        if (!elasticsearchTemplate.indexOps(INDEX_COORDINATES).exists()) {
            elasticsearchTemplate.indexOps(INDEX_COORDINATES).create();
            Document mapping = Document.create();
            mapping.put("properties", Map.of(
                    "id", Map.of("type", "keyword"),
                    "name", Map.of("type", "text"),
                    "email", Map.of("type", "keyword"),
                    "phone", Map.of("type", "keyword"),
                    "address", Map.of("type", "text")
            ));
            elasticsearchTemplate.indexOps(INDEX_COORDINATES).putMapping(mapping);
            log.info("Created customers index with mapping");
        }
    }

    private CustomerDocument customerToDocument(Customer customer) {
        CustomerDocument doc = new CustomerDocument();
        doc.id = customer.getId().toString();
        doc.name = customer.getName();
        doc.email = customer.getEmail();
        doc.phone = customer.getPhone();
        doc.address = customer.getAddress();
        return doc;
    }

    private CustomerSearchResult toSearchResult(SearchHit<CustomerDocument> hit) {
        CustomerDocument doc = hit.getContent();
        return new CustomerSearchResult(
                UUID.fromString(doc.id),
                doc.name,
                doc.email,
                doc.address,
                hit.getScore()
        );
    }

    /**
     * Internal document class for Elasticsearch.
     */
    public static class CustomerDocument {
        public String id;
        public String name;
        public String email;
        public String phone;
        public String address;
    }

    /**
     * Search result DTO.
     */
    public record CustomerSearchResult(
            UUID id,
            String name,
            String email,
            String address,
            float score
    ) {}
}
