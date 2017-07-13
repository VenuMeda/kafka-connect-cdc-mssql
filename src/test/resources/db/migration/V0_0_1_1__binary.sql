CREATE TABLE [dbo].[binary_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    BINARY(128)
);
CREATE TABLE [dbo].[varbinary_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    VARBINARY(256)
);
CREATE TABLE [dbo].[image_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    IMAGE
);

