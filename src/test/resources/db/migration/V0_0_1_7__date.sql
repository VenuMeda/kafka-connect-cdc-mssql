CREATE TABLE [dbo].[date_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    DATE
);
CREATE TABLE [dbo].[datetime_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    DATETIME
);
CREATE TABLE [dbo].[datetime2_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    DATETIME2
);
CREATE TABLE [dbo].[time_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    TIME
);
CREATE TABLE [dbo].[smalldatetime_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    SMALLDATETIME
);

