-- processFlow (jbpm5) provisioning script of postgresql RDBMS
-- JA Bride :  2 March 2011

-- NOTE:  both jbpm process server and human-task tables will be managed by this PostgreSQL RDBMS

alter user postgres with password 'postgres';

create user jbpm with password 'jbpm';
grant all privileges on database jbpm to jbpm;
alter user jbpm with password 'jbpm';

-- jbpm_bam database
create user jbpm_bam with password 'jbpm_bam';
grant all privileges on database jbpm_bam to jbpm_bam;
alter user jbpm_bam with password 'jbpm_bam';

-- guvnor database
create user guvnor with password 'guvnor';
grant all privileges on database guvnor to guvnor;
alter user guvnor with password 'guvnor';
