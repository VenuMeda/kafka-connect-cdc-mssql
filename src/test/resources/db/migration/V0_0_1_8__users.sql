CREATE TABLE [cdc_testing].[dbo].[users] (
  user_id         BIGINT IDENTITY PRIMARY KEY,
  first_name      VARCHAR(256),
  last_name       VARCHAR(256),
  email           VARCHAR(512),
  gender          VARCHAR(6),
  ip_address      VARCHAR(20),
  company_name    VARCHAR(50),
  country_code    VARCHAR(2),
  latitude        DECIMAL(9, 6),
  longitude       DECIMAL(9, 6),
  account_balance DECIMAL(32, 6),
  username        VARCHAR(50)
);
