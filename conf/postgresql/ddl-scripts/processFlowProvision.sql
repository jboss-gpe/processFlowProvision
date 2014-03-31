-- processFlow (jbpm5) provisioning script of postgresql RDBMS
-- JA Bride :  2 March 2011

-- jbpm database
-- NOTE:  both jbpm process server and human-task tables will be managed by this PostgreSQL RDBMS
drop database if exists jbpm;
create database jbpm;
create user jbpm with password 'jbpm';
grant all privileges on database jbpm to jbpm;
alter user jbpm with password 'jbpm';

-- jbpm_bam database
drop database if exists jbpm_bam;
create database jbpm_bam;
create user jbpm_bam with password 'jbpm_bam';
grant all privileges on database jbpm_bam to jbpm_bam;
alter user jbpm_bam with password 'jbpm_bam';
