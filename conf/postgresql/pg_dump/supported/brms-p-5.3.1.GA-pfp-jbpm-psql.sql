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

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: attachment; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE attachment (
    id bigint NOT NULL,
    accesstype integer,
    attachedat timestamp without time zone,
    attachmentcontentid bigint NOT NULL,
    contenttype character varying(255),
    name character varying(255),
    attachment_size integer,
    attachedby_id character varying(255),
    taskdata_attachments_id bigint
);


ALTER TABLE public.attachment OWNER TO jbpm;

--
-- Name: booleanexpression; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE booleanexpression (
    id bigint NOT NULL,
    expression text,
    type character varying(255),
    escalation_constraints_id bigint
);


ALTER TABLE public.booleanexpression OWNER TO jbpm;

--
-- Name: content; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE content (
    id bigint NOT NULL,
    content oid
);


ALTER TABLE public.content OWNER TO jbpm;

--
-- Name: deadline; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE deadline (
    id bigint NOT NULL,
    deadline_date timestamp without time zone,
    escalated smallint,
    deadlines_startdeadline_id bigint,
    deadlines_enddeadline_id bigint
);


ALTER TABLE public.deadline OWNER TO jbpm;

--
-- Name: delegation_delegates; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE delegation_delegates (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.delegation_delegates OWNER TO jbpm;

--
-- Name: email_header; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE email_header (
    id bigint NOT NULL,
    body text,
    fromaddress character varying(255),
    language character varying(255),
    replytoaddress character varying(255),
    subject character varying(255)
);


ALTER TABLE public.email_header OWNER TO jbpm;

--
-- Name: escalation; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE escalation (
    id bigint NOT NULL,
    name character varying(255),
    deadline_escalation_id bigint
);


ALTER TABLE public.escalation OWNER TO jbpm;

--
-- Name: eventtypes; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE eventtypes (
    instanceid bigint NOT NULL,
    eventtypes character varying(255)
);


ALTER TABLE public.eventtypes OWNER TO jbpm;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: jbpm
--

CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO jbpm;

--
-- Name: hibernate_sequence; Type: SEQUENCE SET; Schema: public; Owner: jbpm
--

SELECT pg_catalog.setval('hibernate_sequence', 1, false);


--
-- Name: i18ntext; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE i18ntext (
    id bigint NOT NULL,
    language character varying(255),
    shorttext character varying(255),
    text text,
    task_subjects_id bigint,
    task_names_id bigint,
    task_descriptions_id bigint,
    reassignment_documentation_id bigint,
    notification_subjects_id bigint,
    notification_names_id bigint,
    notification_documentation_id bigint,
    notification_descriptions_id bigint,
    deadline_documentation_id bigint
);


ALTER TABLE public.i18ntext OWNER TO jbpm;

--
-- Name: notification; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE notification (
    dtype character varying(31) NOT NULL,
    id bigint NOT NULL,
    priority integer NOT NULL,
    escalation_notifications_id bigint
);


ALTER TABLE public.notification OWNER TO jbpm;

--
-- Name: notification_bas; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE notification_bas (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.notification_bas OWNER TO jbpm;

--
-- Name: notification_email_header; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE notification_email_header (
    notification_id bigint NOT NULL,
    emailheaders_id bigint NOT NULL,
    mapkey character varying(255) NOT NULL
);


ALTER TABLE public.notification_email_header OWNER TO jbpm;

--
-- Name: notification_recipients; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE notification_recipients (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.notification_recipients OWNER TO jbpm;

--
-- Name: organizationalentity; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE organizationalentity (
    dtype character varying(31) NOT NULL,
    id character varying(255) NOT NULL
);


ALTER TABLE public.organizationalentity OWNER TO jbpm;

--
-- Name: peopleassignments_bas; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE peopleassignments_bas (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.peopleassignments_bas OWNER TO jbpm;

--
-- Name: peopleassignments_exclowners; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE peopleassignments_exclowners (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.peopleassignments_exclowners OWNER TO jbpm;

--
-- Name: peopleassignments_potowners; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE peopleassignments_potowners (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.peopleassignments_potowners OWNER TO jbpm;

--
-- Name: peopleassignments_recipients; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE peopleassignments_recipients (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.peopleassignments_recipients OWNER TO jbpm;

--
-- Name: peopleassignments_stakeholders; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE peopleassignments_stakeholders (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.peopleassignments_stakeholders OWNER TO jbpm;

--
-- Name: processinstanceinfo; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE processinstanceinfo (
    instanceid bigint NOT NULL,
    id bigint,
    lastmodificationdate timestamp without time zone,
    lastreaddate timestamp without time zone,
    processid character varying(255),
    processinstancebytearray oid,
    startdate timestamp without time zone,
    state integer NOT NULL,
    optlock integer
);


ALTER TABLE public.processinstanceinfo OWNER TO jbpm;

--
-- Name: reassignment; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE reassignment (
    id bigint NOT NULL,
    escalation_reassignments_id bigint
);


ALTER TABLE public.reassignment OWNER TO jbpm;

--
-- Name: reassignment_potentialowners; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE reassignment_potentialowners (
    task_id bigint NOT NULL,
    entity_id character varying(255) NOT NULL
);


ALTER TABLE public.reassignment_potentialowners OWNER TO jbpm;

--
-- Name: session_proc_xref_id_seq; Type: SEQUENCE; Schema: public; Owner: jbpm
--

CREATE SEQUENCE session_proc_xref_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.session_proc_xref_id_seq OWNER TO jbpm;

--
-- Name: session_proc_xref_id_seq; Type: SEQUENCE SET; Schema: public; Owner: jbpm
--

SELECT pg_catalog.setval('session_proc_xref_id_seq', 1, false);


--
-- Name: sessioninfo; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE sessioninfo (
    id integer NOT NULL,
    lastmodificationdate timestamp without time zone,
    rulesbytearray oid,
    startdate timestamp without time zone,
    optlock integer
);


ALTER TABLE public.sessioninfo OWNER TO jbpm;

--
-- Name: sessioninfo_id_seq; Type: SEQUENCE; Schema: public; Owner: jbpm
--

CREATE SEQUENCE sessioninfo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sessioninfo_id_seq OWNER TO jbpm;

--
-- Name: sessioninfo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: jbpm
--

SELECT pg_catalog.setval('sessioninfo_id_seq', 1, false);


--
-- Name: sessionprocessxref; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE sessionprocessxref (
    id bigint NOT NULL,
    processid character varying(255),
    processinstanceid bigint,
    sessionid integer,
    status integer NOT NULL,
    optlock integer
);


ALTER TABLE public.sessionprocessxref OWNER TO jbpm;

--
-- Name: subtasksstrategy; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE subtasksstrategy (
    dtype character varying(100) NOT NULL,
    id bigint NOT NULL,
    name character varying(255),
    task_id bigint
);


ALTER TABLE public.subtasksstrategy OWNER TO jbpm;

--
-- Name: task; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE task (
    id bigint NOT NULL,
    archived smallint,
    allowedtodelegate character varying(255),
    priority integer NOT NULL,
    activationtime timestamp without time zone,
    completedon timestamp without time zone,
    createdon timestamp without time zone,
    documentaccesstype integer,
    documentcontentid bigint NOT NULL,
    documenttype character varying(255),
    expirationtime timestamp without time zone,
    faultaccesstype integer,
    faultcontentid bigint NOT NULL,
    faultname character varying(255),
    faulttype character varying(255),
    outputaccesstype integer,
    outputcontentid bigint NOT NULL,
    outputtype character varying(255),
    parentid bigint NOT NULL,
    previousstatus integer,
    processid character varying(255),
    processinstanceid bigint NOT NULL,
    processsessionid integer NOT NULL,
    skipable boolean NOT NULL,
    status character varying(255),
    workitemid bigint NOT NULL,
    optlock integer,
    taskinitiator_id character varying(255),
    actualowner_id character varying(255),
    createdby_id character varying(255)
);


ALTER TABLE public.task OWNER TO jbpm;

--
-- Name: task_comment; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE task_comment (
    id bigint NOT NULL,
    addedat timestamp without time zone,
    text text,
    addedby_id character varying(255),
    taskdata_comments_id bigint
);


ALTER TABLE public.task_comment OWNER TO jbpm;

--
-- Name: workiteminfo; Type: TABLE; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE TABLE workiteminfo (
    workitemid bigint NOT NULL,
    creationdate timestamp without time zone,
    name character varying(255),
    processinstanceid bigint NOT NULL,
    state bigint NOT NULL,
    optlock integer,
    workitembytearray oid
);


ALTER TABLE public.workiteminfo OWNER TO jbpm;

--
-- Name: workiteminfo_id_seq; Type: SEQUENCE; Schema: public; Owner: jbpm
--

CREATE SEQUENCE workiteminfo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.workiteminfo_id_seq OWNER TO jbpm;

--
-- Name: workiteminfo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: jbpm
--

SELECT pg_catalog.setval('workiteminfo_id_seq', 1, false);


--
-- Data for Name: attachment; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY attachment (id, accesstype, attachedat, attachmentcontentid, contenttype, name, attachment_size, attachedby_id, taskdata_attachments_id) FROM stdin;
\.


--
-- Data for Name: booleanexpression; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY booleanexpression (id, expression, type, escalation_constraints_id) FROM stdin;
\.


--
-- Data for Name: content; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY content (id, content) FROM stdin;
\.


--
-- Data for Name: deadline; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY deadline (id, deadline_date, escalated, deadlines_startdeadline_id, deadlines_enddeadline_id) FROM stdin;
\.


--
-- Data for Name: delegation_delegates; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY delegation_delegates (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: email_header; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY email_header (id, body, fromaddress, language, replytoaddress, subject) FROM stdin;
\.


--
-- Data for Name: escalation; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY escalation (id, name, deadline_escalation_id) FROM stdin;
\.


--
-- Data for Name: eventtypes; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY eventtypes (instanceid, eventtypes) FROM stdin;
\.


--
-- Data for Name: i18ntext; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY i18ntext (id, language, shorttext, text, task_subjects_id, task_names_id, task_descriptions_id, reassignment_documentation_id, notification_subjects_id, notification_names_id, notification_documentation_id, notification_descriptions_id, deadline_documentation_id) FROM stdin;
\.


--
-- Data for Name: notification; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY notification (dtype, id, priority, escalation_notifications_id) FROM stdin;
\.


--
-- Data for Name: notification_bas; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY notification_bas (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: notification_email_header; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY notification_email_header (notification_id, emailheaders_id, mapkey) FROM stdin;
\.


--
-- Data for Name: notification_recipients; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY notification_recipients (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: organizationalentity; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY organizationalentity (dtype, id) FROM stdin;
\.


--
-- Data for Name: peopleassignments_bas; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY peopleassignments_bas (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: peopleassignments_exclowners; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY peopleassignments_exclowners (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: peopleassignments_potowners; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY peopleassignments_potowners (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: peopleassignments_recipients; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY peopleassignments_recipients (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: peopleassignments_stakeholders; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY peopleassignments_stakeholders (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: processinstanceinfo; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY processinstanceinfo (instanceid, id, lastmodificationdate, lastreaddate, processid, processinstancebytearray, startdate, state, optlock) FROM stdin;
\.


--
-- Data for Name: reassignment; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY reassignment (id, escalation_reassignments_id) FROM stdin;
\.


--
-- Data for Name: reassignment_potentialowners; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY reassignment_potentialowners (task_id, entity_id) FROM stdin;
\.


--
-- Data for Name: sessioninfo; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY sessioninfo (id, lastmodificationdate, rulesbytearray, startdate, optlock) FROM stdin;
\.


--
-- Data for Name: sessionprocessxref; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY sessionprocessxref (id, processid, processinstanceid, sessionid, status, optlock) FROM stdin;
\.


--
-- Data for Name: subtasksstrategy; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY subtasksstrategy (dtype, id, name, task_id) FROM stdin;
\.


--
-- Data for Name: task; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY task (id, archived, allowedtodelegate, priority, activationtime, completedon, createdon, documentaccesstype, documentcontentid, documenttype, expirationtime, faultaccesstype, faultcontentid, faultname, faulttype, outputaccesstype, outputcontentid, outputtype, parentid, previousstatus, processid, processinstanceid, processsessionid, skipable, status, workitemid, optlock, taskinitiator_id, actualowner_id, createdby_id) FROM stdin;
\.


--
-- Data for Name: task_comment; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY task_comment (id, addedat, text, addedby_id, taskdata_comments_id) FROM stdin;
\.


--
-- Data for Name: workiteminfo; Type: TABLE DATA; Schema: public; Owner: jbpm
--

COPY workiteminfo (workitemid, creationdate, name, processinstanceid, state, optlock, workitembytearray) FROM stdin;
\.


--
-- Name: attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY attachment
    ADD CONSTRAINT attachment_pkey PRIMARY KEY (id);


--
-- Name: booleanexpression_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY booleanexpression
    ADD CONSTRAINT booleanexpression_pkey PRIMARY KEY (id);


--
-- Name: content_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY content
    ADD CONSTRAINT content_pkey PRIMARY KEY (id);


--
-- Name: deadline_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY deadline
    ADD CONSTRAINT deadline_pkey PRIMARY KEY (id);


--
-- Name: email_header_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY email_header
    ADD CONSTRAINT email_header_pkey PRIMARY KEY (id);


--
-- Name: escalation_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY escalation
    ADD CONSTRAINT escalation_pkey PRIMARY KEY (id);


--
-- Name: i18ntext_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT i18ntext_pkey PRIMARY KEY (id);


--
-- Name: notification_email_header_emailheaders_id_key; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY notification_email_header
    ADD CONSTRAINT notification_email_header_emailheaders_id_key UNIQUE (emailheaders_id);


--
-- Name: notification_email_header_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY notification_email_header
    ADD CONSTRAINT notification_email_header_pkey PRIMARY KEY (notification_id, mapkey);


--
-- Name: notification_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: organizationalentity_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY organizationalentity
    ADD CONSTRAINT organizationalentity_pkey PRIMARY KEY (id);


--
-- Name: processinstanceinfo_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY processinstanceinfo
    ADD CONSTRAINT processinstanceinfo_pkey PRIMARY KEY (instanceid);


--
-- Name: reassignment_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY reassignment
    ADD CONSTRAINT reassignment_pkey PRIMARY KEY (id);


--
-- Name: sessioninfo_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY sessioninfo
    ADD CONSTRAINT sessioninfo_pkey PRIMARY KEY (id);


--
-- Name: sessionprocessxref_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY sessionprocessxref
    ADD CONSTRAINT sessionprocessxref_pkey PRIMARY KEY (id);


--
-- Name: subtasksstrategy_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY subtasksstrategy
    ADD CONSTRAINT subtasksstrategy_pkey PRIMARY KEY (id);


--
-- Name: task_comment_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY task_comment
    ADD CONSTRAINT task_comment_pkey PRIMARY KEY (id);


--
-- Name: task_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY task
    ADD CONSTRAINT task_pkey PRIMARY KEY (id);


--
-- Name: workiteminfo_pkey; Type: CONSTRAINT; Schema: public; Owner: jbpm; Tablespace: 
--

ALTER TABLE ONLY workiteminfo
    ADD CONSTRAINT workiteminfo_pkey PRIMARY KEY (workitemid);


--
-- Name: idx_eventtypes; Type: INDEX; Schema: public; Owner: jbpm; Tablespace: 
--

CREATE INDEX idx_eventtypes ON eventtypes USING btree (instanceid);


--
-- Name: fk1c935438ef5f064; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY attachment
    ADD CONSTRAINT fk1c935438ef5f064 FOREIGN KEY (attachedby_id) REFERENCES organizationalentity(id);


--
-- Name: fk1c93543f21826d9; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY attachment
    ADD CONSTRAINT fk1c93543f21826d9 FOREIGN KEY (taskdata_attachments_id) REFERENCES task(id);


--
-- Name: fk1ee418d2c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_potowners
    ADD CONSTRAINT fk1ee418d2c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fk1ee418d36b2f154; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_potowners
    ADD CONSTRAINT fk1ee418d36b2f154 FOREIGN KEY (task_id) REFERENCES task(id);


--
-- Name: fk21df3e7827abeb8a; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY deadline
    ADD CONSTRAINT fk21df3e7827abeb8a FOREIGN KEY (deadlines_enddeadline_id) REFERENCES task(id);


--
-- Name: fk21df3e78684baca3; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY deadline
    ADD CONSTRAINT fk21df3e78684baca3 FOREIGN KEY (deadlines_startdeadline_id) REFERENCES task(id);


--
-- Name: fk2349686b2162dfb4; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686b2162dfb4 FOREIGN KEY (notification_descriptions_id) REFERENCES notification(id);


--
-- Name: fk2349686b3330f6d9; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686b3330f6d9 FOREIGN KEY (deadline_documentation_id) REFERENCES deadline(id);


--
-- Name: fk2349686b5eebb6d9; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686b5eebb6d9 FOREIGN KEY (reassignment_documentation_id) REFERENCES reassignment(id);


--
-- Name: fk2349686b69b21ee8; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686b69b21ee8 FOREIGN KEY (task_descriptions_id) REFERENCES task(id);


--
-- Name: fk2349686b8046a239; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686b8046a239 FOREIGN KEY (notification_documentation_id) REFERENCES notification(id);


--
-- Name: fk2349686b98b62b; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686b98b62b FOREIGN KEY (task_names_id) REFERENCES task(id);


--
-- Name: fk2349686bb2fa6b18; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686bb2fa6b18 FOREIGN KEY (task_subjects_id) REFERENCES task(id);


--
-- Name: fk2349686bd488ceeb; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686bd488ceeb FOREIGN KEY (notification_names_id) REFERENCES notification(id);


--
-- Name: fk2349686bf952cee4; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY i18ntext
    ADD CONSTRAINT fk2349686bf952cee4 FOREIGN KEY (notification_subjects_id) REFERENCES notification(id);


--
-- Name: fk27a9a56ce1ef3a; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY task
    ADD CONSTRAINT fk27a9a56ce1ef3a FOREIGN KEY (actualowner_id) REFERENCES organizationalentity(id);


--
-- Name: fk27a9a59e619a0; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY task
    ADD CONSTRAINT fk27a9a59e619a0 FOREIGN KEY (createdby_id) REFERENCES organizationalentity(id);


--
-- Name: fk27a9a5f213f8b5; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY task
    ADD CONSTRAINT fk27a9a5f213f8b5 FOREIGN KEY (taskinitiator_id) REFERENCES organizationalentity(id);


--
-- Name: fk2d45dd0b3e0890b; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY notification
    ADD CONSTRAINT fk2d45dd0b3e0890b FOREIGN KEY (escalation_notifications_id) REFERENCES escalation(id);


--
-- Name: fk2dd68ee02c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY notification_bas
    ADD CONSTRAINT fk2dd68ee02c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fk2dd68ee09c76eaba; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY notification_bas
    ADD CONSTRAINT fk2dd68ee09c76eaba FOREIGN KEY (task_id) REFERENCES notification(id);


--
-- Name: fk47485d572c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY delegation_delegates
    ADD CONSTRAINT fk47485d572c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fk47485d5736b2f154; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY delegation_delegates
    ADD CONSTRAINT fk47485d5736b2f154 FOREIGN KEY (task_id) REFERENCES task(id);


--
-- Name: fk482f79d52c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_stakeholders
    ADD CONSTRAINT fk482f79d52c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fk482f79d536b2f154; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_stakeholders
    ADD CONSTRAINT fk482f79d536b2f154 FOREIGN KEY (task_id) REFERENCES task(id);


--
-- Name: fk61f475a52ff04688; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY task_comment
    ADD CONSTRAINT fk61f475a52ff04688 FOREIGN KEY (addedby_id) REFERENCES organizationalentity(id);


--
-- Name: fk61f475a5b35e68f5; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY task_comment
    ADD CONSTRAINT fk61f475a5b35e68f5 FOREIGN KEY (taskdata_comments_id) REFERENCES task(id);


--
-- Name: fk67b2c6b5c7a04c70; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY escalation
    ADD CONSTRAINT fk67b2c6b5c7a04c70 FOREIGN KEY (deadline_escalation_id) REFERENCES deadline(id);


--
-- Name: fk724d0560a5c17ee0; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY reassignment
    ADD CONSTRAINT fk724d0560a5c17ee0 FOREIGN KEY (escalation_reassignments_id) REFERENCES escalation(id);


--
-- Name: fk90b59cff2c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY reassignment_potentialowners
    ADD CONSTRAINT fk90b59cff2c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fk90b59cffe17e130f; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY reassignment_potentialowners
    ADD CONSTRAINT fk90b59cffe17e130f FOREIGN KEY (task_id) REFERENCES reassignment(id);


--
-- Name: fk98fd214e2c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY notification_recipients
    ADD CONSTRAINT fk98fd214e2c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fk98fd214e9c76eaba; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY notification_recipients
    ADD CONSTRAINT fk98fd214e9c76eaba FOREIGN KEY (task_id) REFERENCES notification(id);


--
-- Name: fk9d8cf4ec2c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_bas
    ADD CONSTRAINT fk9d8cf4ec2c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fk9d8cf4ec36b2f154; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_bas
    ADD CONSTRAINT fk9d8cf4ec36b2f154 FOREIGN KEY (task_id) REFERENCES task(id);


--
-- Name: fkb0e5621f7665489a; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY eventtypes
    ADD CONSTRAINT fkb0e5621f7665489a FOREIGN KEY (instanceid) REFERENCES processinstanceinfo(instanceid);


--
-- Name: fkc6f615c22c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_recipients
    ADD CONSTRAINT fkc6f615c22c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fkc6f615c236b2f154; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_recipients
    ADD CONSTRAINT fkc6f615c236b2f154 FOREIGN KEY (task_id) REFERENCES task(id);


--
-- Name: fkc77b97e42c122ed2; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_exclowners
    ADD CONSTRAINT fkc77b97e42c122ed2 FOREIGN KEY (entity_id) REFERENCES organizationalentity(id);


--
-- Name: fkc77b97e436b2f154; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY peopleassignments_exclowners
    ADD CONSTRAINT fkc77b97e436b2f154 FOREIGN KEY (task_id) REFERENCES task(id);


--
-- Name: fkde5df2e136b2f154; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY subtasksstrategy
    ADD CONSTRAINT fkde5df2e136b2f154 FOREIGN KEY (task_id) REFERENCES task(id);


--
-- Name: fke3d208c0afb75f7d; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY booleanexpression
    ADD CONSTRAINT fke3d208c0afb75f7d FOREIGN KEY (escalation_constraints_id) REFERENCES escalation(id);


--
-- Name: fkf30fe3441f7b912a; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY notification_email_header
    ADD CONSTRAINT fkf30fe3441f7b912a FOREIGN KEY (emailheaders_id) REFERENCES email_header(id);


--
-- Name: fkf30fe34430be501c; Type: FK CONSTRAINT; Schema: public; Owner: jbpm
--

ALTER TABLE ONLY notification_email_header
    ADD CONSTRAINT fkf30fe34430be501c FOREIGN KEY (notification_id) REFERENCES notification(id);


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

