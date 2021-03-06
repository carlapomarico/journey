package com.lisihocke.journey.web.rest;

import com.lisihocke.journey.JourneyApp;
import com.lisihocke.journey.domain.JournalEntry;
import com.lisihocke.journey.repository.JournalEntryRepository;
import com.lisihocke.journey.service.JournalEntryService;
import com.lisihocke.journey.service.dto.JournalEntryDTO;
import com.lisihocke.journey.service.mapper.JournalEntryMapper;
import com.lisihocke.journey.web.rest.errors.ExceptionTranslator;
import com.lisihocke.journey.service.dto.JournalEntryCriteria;
import com.lisihocke.journey.service.JournalEntryQueryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;

import static com.lisihocke.journey.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@Link JournalEntryResource} REST controller.
 */
@SpringBootTest(classes = JourneyApp.class)
public class JournalEntryResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalEntryMapper journalEntryMapper;

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private JournalEntryQueryService journalEntryQueryService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restJournalEntryMockMvc;

    private JournalEntry journalEntry;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final JournalEntryResource journalEntryResource = new JournalEntryResource(journalEntryService, journalEntryQueryService);
        this.restJournalEntryMockMvc = MockMvcBuilders.standaloneSetup(journalEntryResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static JournalEntry createEntity(EntityManager em) {
        JournalEntry journalEntry = new JournalEntry()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION);
        return journalEntry;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static JournalEntry createUpdatedEntity(EntityManager em) {
        JournalEntry journalEntry = new JournalEntry()
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION);
        return journalEntry;
    }

    @BeforeEach
    public void initTest() {
        journalEntry = createEntity(em);
    }

    @Test
    @Transactional
    public void createJournalEntry() throws Exception {
        int databaseSizeBeforeCreate = journalEntryRepository.findAll().size();

        // Create the JournalEntry
        JournalEntryDTO journalEntryDTO = journalEntryMapper.toDto(journalEntry);
        restJournalEntryMockMvc.perform(post("/api/journal-entries")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(journalEntryDTO)))
            .andExpect(status().isCreated());

        // Validate the JournalEntry in the database
        List<JournalEntry> journalEntryList = journalEntryRepository.findAll();
        assertThat(journalEntryList).hasSize(databaseSizeBeforeCreate + 1);
        JournalEntry testJournalEntry = journalEntryList.get(journalEntryList.size() - 1);
        assertThat(testJournalEntry.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testJournalEntry.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createJournalEntryWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = journalEntryRepository.findAll().size();

        // Create the JournalEntry with an existing ID
        journalEntry.setId(1L);
        JournalEntryDTO journalEntryDTO = journalEntryMapper.toDto(journalEntry);

        // An entity with an existing ID cannot be created, so this API call must fail
        restJournalEntryMockMvc.perform(post("/api/journal-entries")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(journalEntryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the JournalEntry in the database
        List<JournalEntry> journalEntryList = journalEntryRepository.findAll();
        assertThat(journalEntryList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkTitleIsRequired() throws Exception {
        int databaseSizeBeforeTest = journalEntryRepository.findAll().size();
        // set the field null
        journalEntry.setTitle(null);

        // Create the JournalEntry, which fails.
        JournalEntryDTO journalEntryDTO = journalEntryMapper.toDto(journalEntry);

        restJournalEntryMockMvc.perform(post("/api/journal-entries")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(journalEntryDTO)))
            .andExpect(status().isBadRequest());

        List<JournalEntry> journalEntryList = journalEntryRepository.findAll();
        assertThat(journalEntryList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllJournalEntries() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        // Get all the journalEntryList
        restJournalEntryMockMvc.perform(get("/api/journal-entries?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(journalEntry.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())));
    }
    
    @Test
    @Transactional
    public void getJournalEntry() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        // Get the journalEntry
        restJournalEntryMockMvc.perform(get("/api/journal-entries/{id}", journalEntry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(journalEntry.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()));
    }

    @Test
    @Transactional
    public void getAllJournalEntriesByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        // Get all the journalEntryList where title equals to DEFAULT_TITLE
        defaultJournalEntryShouldBeFound("title.equals=" + DEFAULT_TITLE);

        // Get all the journalEntryList where title equals to UPDATED_TITLE
        defaultJournalEntryShouldNotBeFound("title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    public void getAllJournalEntriesByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        // Get all the journalEntryList where title in DEFAULT_TITLE or UPDATED_TITLE
        defaultJournalEntryShouldBeFound("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE);

        // Get all the journalEntryList where title equals to UPDATED_TITLE
        defaultJournalEntryShouldNotBeFound("title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    public void getAllJournalEntriesByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        // Get all the journalEntryList where title is not null
        defaultJournalEntryShouldBeFound("title.specified=true");

        // Get all the journalEntryList where title is null
        defaultJournalEntryShouldNotBeFound("title.specified=false");
    }

    @Test
    @Transactional
    public void getAllJournalEntriesByDescriptionIsEqualToSomething() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        // Get all the journalEntryList where description equals to DEFAULT_DESCRIPTION
        defaultJournalEntryShouldBeFound("description.equals=" + DEFAULT_DESCRIPTION);

        // Get all the journalEntryList where description equals to UPDATED_DESCRIPTION
        defaultJournalEntryShouldNotBeFound("description.equals=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    public void getAllJournalEntriesByDescriptionIsInShouldWork() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        // Get all the journalEntryList where description in DEFAULT_DESCRIPTION or UPDATED_DESCRIPTION
        defaultJournalEntryShouldBeFound("description.in=" + DEFAULT_DESCRIPTION + "," + UPDATED_DESCRIPTION);

        // Get all the journalEntryList where description equals to UPDATED_DESCRIPTION
        defaultJournalEntryShouldNotBeFound("description.in=" + UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    public void getAllJournalEntriesByDescriptionIsNullOrNotNull() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        // Get all the journalEntryList where description is not null
        defaultJournalEntryShouldBeFound("description.specified=true");

        // Get all the journalEntryList where description is null
        defaultJournalEntryShouldNotBeFound("description.specified=false");
    }
    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultJournalEntryShouldBeFound(String filter) throws Exception {
        restJournalEntryMockMvc.perform(get("/api/journal-entries?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(journalEntry.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)));

        // Check, that the count call also returns 1
        restJournalEntryMockMvc.perform(get("/api/journal-entries/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultJournalEntryShouldNotBeFound(String filter) throws Exception {
        restJournalEntryMockMvc.perform(get("/api/journal-entries?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restJournalEntryMockMvc.perform(get("/api/journal-entries/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("0"));
    }


    @Test
    @Transactional
    public void getNonExistingJournalEntry() throws Exception {
        // Get the journalEntry
        restJournalEntryMockMvc.perform(get("/api/journal-entries/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateJournalEntry() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        int databaseSizeBeforeUpdate = journalEntryRepository.findAll().size();

        // Update the journalEntry
        JournalEntry updatedJournalEntry = journalEntryRepository.findById(journalEntry.getId()).get();
        // Disconnect from session so that the updates on updatedJournalEntry are not directly saved in db
        em.detach(updatedJournalEntry);
        updatedJournalEntry
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION);
        JournalEntryDTO journalEntryDTO = journalEntryMapper.toDto(updatedJournalEntry);

        restJournalEntryMockMvc.perform(put("/api/journal-entries")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(journalEntryDTO)))
            .andExpect(status().isOk());

        // Validate the JournalEntry in the database
        List<JournalEntry> journalEntryList = journalEntryRepository.findAll();
        assertThat(journalEntryList).hasSize(databaseSizeBeforeUpdate);
        JournalEntry testJournalEntry = journalEntryList.get(journalEntryList.size() - 1);
        assertThat(testJournalEntry.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testJournalEntry.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    public void updateNonExistingJournalEntry() throws Exception {
        int databaseSizeBeforeUpdate = journalEntryRepository.findAll().size();

        // Create the JournalEntry
        JournalEntryDTO journalEntryDTO = journalEntryMapper.toDto(journalEntry);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restJournalEntryMockMvc.perform(put("/api/journal-entries")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(journalEntryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the JournalEntry in the database
        List<JournalEntry> journalEntryList = journalEntryRepository.findAll();
        assertThat(journalEntryList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteJournalEntry() throws Exception {
        // Initialize the database
        journalEntryRepository.saveAndFlush(journalEntry);

        int databaseSizeBeforeDelete = journalEntryRepository.findAll().size();

        // Delete the journalEntry
        restJournalEntryMockMvc.perform(delete("/api/journal-entries/{id}", journalEntry.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database is empty
        List<JournalEntry> journalEntryList = journalEntryRepository.findAll();
        assertThat(journalEntryList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(JournalEntry.class);
        JournalEntry journalEntry1 = new JournalEntry();
        journalEntry1.setId(1L);
        JournalEntry journalEntry2 = new JournalEntry();
        journalEntry2.setId(journalEntry1.getId());
        assertThat(journalEntry1).isEqualTo(journalEntry2);
        journalEntry2.setId(2L);
        assertThat(journalEntry1).isNotEqualTo(journalEntry2);
        journalEntry1.setId(null);
        assertThat(journalEntry1).isNotEqualTo(journalEntry2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(JournalEntryDTO.class);
        JournalEntryDTO journalEntryDTO1 = new JournalEntryDTO();
        journalEntryDTO1.setId(1L);
        JournalEntryDTO journalEntryDTO2 = new JournalEntryDTO();
        assertThat(journalEntryDTO1).isNotEqualTo(journalEntryDTO2);
        journalEntryDTO2.setId(journalEntryDTO1.getId());
        assertThat(journalEntryDTO1).isEqualTo(journalEntryDTO2);
        journalEntryDTO2.setId(2L);
        assertThat(journalEntryDTO1).isNotEqualTo(journalEntryDTO2);
        journalEntryDTO1.setId(null);
        assertThat(journalEntryDTO1).isNotEqualTo(journalEntryDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(journalEntryMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(journalEntryMapper.fromId(null)).isNull();
    }
}
