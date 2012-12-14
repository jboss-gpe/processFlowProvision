--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: jbpm_bam
--

CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO jbpm_bam;

--
-- Name: hibernate_sequence; Type: SEQUENCE SET; Schema: public; Owner: jbpm_bam
--

SELECT pg_catalog.setval('hibernate_sequence', 1, false);


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: humantasklog; Type: TABLE; Schema: public; Owner: jbpm_bam; Tablespace: 
--

CREATE TABLE humantasklog (
    id bigint NOT NULL,
    nodeinstanceid bigint NOT NULL,
    processinstanceid bigint NOT NULL,
    workitemid bigint NOT NULL
);


ALTER TABLE public.humantasklog OWNER TO jbpm_bam;

--
-- Name: nodeinstancelog; Type: TABLE; Schema: public; Owner: jbpm_bam; Tablespace: 
--

CREATE TABLE nodeinstancelog (
    id bigint NOT NULL,
    log_date timestamp without time zone,
    nodeid character varying(255),
    nodeinstanceid character varying(255),
    nodename character varying(255),
    processid character varying(255),
    processinstanceid bigint NOT NULL,
    type integer NOT NULL
);


ALTER TABLE public.nodeinstancelog OWNER TO jbpm_bam;

--
-- Name: processinstancelog; Type: TABLE; Schema: public; Owner: jbpm_bam; Tablespace: 
--

CREATE TABLE processinstancelog (
    id bigint NOT NULL,
    end_date timestamp without time zone,
    processid character varying(255),
    processinstanceid bigint NOT NULL,
    start_date timestamp without time zone
);


ALTER TABLE public.processinstancelog OWNER TO jbpm_bam;

--
-- Name: subprocessinstancelog; Type: TABLE; Schema: public; Owner: jbpm_bam; Tablespace: 
--

CREATE TABLE subprocessinstancelog (
    id bigint NOT NULL,
    parentprocessinstanceid bigint NOT NULL,
    subprocessinstanceid bigint NOT NULL,
    subprocessnodeinstanceid bigint NOT NULL
);


ALTER TABLE public.subprocessinstancelog OWNER TO jbpm_bam;

--
-- Name: variableinstancelog; Type: TABLE; Schema: public; Owner: jbpm_bam; Tablespace: 
--

CREATE TABLE variableinstancelog (
    id bigint NOT NULL,
    log_date timestamp without time zone,
    processid character varying(255),
    processinstanceid bigint NOT NULL,
    value character varying(255),
    variableid character varying(255),
    variableinstanceid character varying(255)
);


ALTER TABLE public.variableinstancelog OWNER TO jbpm_bam;

--
-- Data for Name: humantasklog; Type: TABLE DATA; Schema: public; Owner: jbpm_bam
--

COPY humantasklog (id, nodeinstanceid, processinstanceid, workitemid) FROM stdin;
\.


--
-- Data for Name: nodeinstancelog; Type: TABLE DATA; Schema: public; Owner: jbpm_bam
--

COPY nodeinstancelog (id, log_date, nodeid, nodeinstanceid, nodename, processid, processinstanceid, type) FROM stdin;
\.


--
-- Data for Name: processinstancelog; Type: TABLE DATA; Schema: public; Owner: jbpm_bam
--

COPY processinstancelog (id, end_date, processid, processinstanceid, start_date) FROM stdin;
\.


--
-- Data for Name: subprocessinstancelog; Type: TABLE DATA; Schema: public; Owner: jbpm_bam
--

COPY subprocessinstancelog (id, parentprocessinstanceid, subprocessinstanceid, subprocessnodeinstanceid) FROM stdin;
\.


--
-- Data for Name: variableinstancelog; Type: TABLE DATA; Schema: public; Owner: jbpm_bam
--

COPY variableinstancelog (id, log_date, processid, processinstanceid, value, variableid, variableinstanceid) FROM stdin;
\.


--
-- Name: humantasklog_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm_bam; Tablespace: 
--

ALTER TABLE ONLY humantasklog
    ADD CONSTRAINT humantasklog_pkey PRIMARY KEY (id);


--
-- Name: nodeinstancelog_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm_bam; Tablespace: 
--

ALTER TABLE ONLY nodeinstancelog
    ADD CONSTRAINT nodeinstancelog_pkey PRIMARY KEY (id);


--
-- Name: processinstancelog_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm_bam; Tablespace: 
--

ALTER TABLE ONLY processinstancelog
    ADD CONSTRAINT processinstancelog_pkey PRIMARY KEY (id);


--
-- Name: subprocessinstancelog_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm_bam; Tablespace: 
--

ALTER TABLE ONLY subprocessinstancelog
    ADD CONSTRAINT subprocessinstancelog_pkey PRIMARY KEY (id);


--
-- Name: variableinstancelog_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm_bam; Tablespace: 
--

ALTER TABLE ONLY variableinstancelog
    ADD CONSTRAINT variableinstancelog_pkey PRIMARY KEY (id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

